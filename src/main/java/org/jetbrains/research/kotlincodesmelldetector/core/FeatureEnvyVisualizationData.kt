package org.jetbrains.research.kotlincodesmelldetector.core

import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ClassEntity

class FeatureEnvyVisualizationData(
    private val sourceClass: ClassEntity,
    val methodToBeMoved: KtNamedFunction,
    private val targetClass: ClassEntity
) : VisualizationData {
    override val distinctSourceDependencies: Int
    override val distinctTargetDependencies: Int
    val sourceAssignments: Int
    val containsSuperInvocation: Boolean
    val sourceAccessedMembers = mutableSetOf<KtNamedDeclaration>()
    private val targetAccessedMembers = mutableSetOf<KtNamedDeclaration>()
    private val definedSourceFields = mutableSetOf<KtNamedDeclaration>()

    init {
        var containsSuperInvocation = false
        methodToBeMoved.bodyExpression?.forEachDescendantOfType<KtReferenceExpression> { expression ->
            val parent = expression.parent
            if (parent is KtQualifiedExpression) {
                val receiverExpression = parent.receiverExpression
                if (receiverExpression != expression) {
                    when (receiverExpression) {
                        is KtThisExpression -> {
                            handlePossibleSourceMember(expression)
                        }
                        is KtSuperExpression -> {
                            containsSuperInvocation = true
                        }
                        else -> {
                            val receiver =
                                if (receiverExpression is KtQualifiedExpression) {
                                    if (receiverExpression.receiverExpression is KtThisExpression)
                                        receiverExpression.selectorExpression else null
                                } else {
                                    receiverExpression
                                }
                            receiver?.mainReference?.resolve()?.let { resolvedReceiver ->
                                if (resolvedReceiver is KtNamedDeclaration
                                    && (resolvedReceiver.type()?.fqName == targetClass.element.fqName
                                        || resolvedReceiver.fqName == targetClass.element.fqName)
                                ) {
                                    handlePossibleTargetMember(expression)
                                }
                            }
                        }
                    }
                } else if (receiverExpression is KtNameReferenceExpression) {
                    handlePossibleSourceMember(receiverExpression)
                }
            } else if (parent !is KtCallExpression) {
                handlePossibleSourceMember(expression)
            }
        }
        this.containsSuperInvocation = containsSuperInvocation
        distinctSourceDependencies = sourceAccessedMembers.size
        distinctTargetDependencies = targetAccessedMembers.size
        sourceAssignments = definedSourceFields.size
    }

    private fun handlePossibleSourceMember(expression: KtReferenceExpression) {
        val reference = when (expression) {
            is KtCallExpression -> expression.calleeExpression?.mainReference
            is KtNameReferenceExpression -> expression.mainReference
            else -> null
        }
        reference?.resolve()?.let { called ->
            if (called is KtNamedDeclaration) {
                if (called in targetClass.attributeList || called in targetClass.methodList) {
                    targetAccessedMembers.add(called)
                } else if (called in sourceClass.attributeList || called in sourceClass.methodList) {
                    sourceAccessedMembers.add(called)
                    if (fieldIsDefined(expression)) {
                        definedSourceFields.add(called)
                    }
                }
            }
        }
    }

    private fun handlePossibleTargetMember(expression: KtReferenceExpression) {
        val reference = when (expression) {
            is KtCallExpression -> expression.calleeExpression?.mainReference
            is KtNameReferenceExpression -> expression.mainReference
            else -> null
        }
        reference?.resolve()?.let { called ->
            if (called is KtNamedDeclaration && (called in targetClass.attributeList || called in targetClass.methodList)) {
                targetAccessedMembers.add(called)
            }
        }
    }

    private fun fieldIsDefined(expression: KtReferenceExpression): Boolean {
        var parent = expression.parent
        if (parent is KtQualifiedExpression && parent.receiverExpression is KtThisExpression) {
            parent = parent.parent
        }
        if (parent is KtBinaryExpression && parent.operationToken == KtTokens.EQ) {
            return true
        }
        return false
    }
}
