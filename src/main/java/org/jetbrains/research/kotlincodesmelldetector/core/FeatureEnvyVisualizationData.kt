package org.jetbrains.research.kotlincodesmelldetector.core

import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ClassEntity
import org.jetbrains.research.kotlincodesmelldetector.utils.nameWithParameterList

class FeatureEnvyVisualizationData(private val sourceClass: ClassEntity, methodToBeMoved: KtNamedFunction, private val targetClass: ClassEntity) : VisualizationData {
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
                            receiverExpression.mainReference?.resolve()?.let { receiver ->
                                if (receiver is KtNamedDeclaration && (receiver.type()?.constructor?.declarationDescriptor?.fqNameOrNull() == targetClass.fqName
                                                || receiver.fqName == targetClass.fqName)) {
                                    handlePossibleTargetMember(expression)
                                }
                            }
                        }
                    }
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
            else -> expression.mainReference
        }
        reference?.resolve()?.let { called ->
            val name = if (called is KtNamedFunction) called.nameWithParameterList else called.getKotlinFqName()
            if (name != null && called is KtNamedDeclaration) {
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
            else -> expression.mainReference
        }
        reference?.resolve()?.let { called ->
            val name = if (called is KtNamedFunction) called.nameWithParameterList else called.getKotlinFqName()
            name?.let {
                if (called is KtNamedDeclaration && (called in targetClass.attributeList || called in targetClass.methodList)) {
                    targetAccessedMembers.add(called)
                }
            }
        }
    }

    private fun fieldIsDefined(expression: KtExpression): Boolean {
        if (expression is KtReferenceExpression) {
            var parent = expression.parent
            if (parent is KtQualifiedExpression) {
                parent = parent.parent
            }
            if (parent is KtBinaryExpression && parent.operationToken == KtTokens.EQ) {
                return true
            }
        }
        return false
    }


}
