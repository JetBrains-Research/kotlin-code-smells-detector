package org.jetbrains.research.kotlincodesmelldetector.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.research.kotlincodesmelldetector.utils.pseudoUsedThroughThisReference
import org.jetbrains.research.kotlincodesmelldetector.utils.referencesInBody
import org.jetbrains.research.kotlincodesmelldetector.utils.resolveToElement
import org.jetbrains.research.kotlincodesmelldetector.utils.usedThroughThisReference

class GodClassVisualizationData(
    extractedMethods: Set<KtDeclaration>,
    extractedFields: Set<KtDeclaration>
) : VisualizationData {

    override val distinctTargetDependencies: Int

    override val distinctSourceDependencies: Int

    val pseudoDistinctTargetDependencies: Int

    val pseudoDistinctSourceDependencies: Int

    init {
        val distinctTargetDependencies = mutableSetOf<PsiElement>()
        val distinctSourceDependencies = mutableSetOf<PsiElement>()

        val pseudoDistinctTargetDependencies = mutableSetOf<PsiElement>()
        val pseudoDistinctSourceDependencies = mutableSetOf<PsiElement>()

        var pseudoDistinctTargetDependencies2 = 0
        var pseudoDistinctSourceDependencies2 = 0

        for (function in extractedMethods) {
            pseudoDistinctSourceDependencies.clear()
            pseudoDistinctTargetDependencies.clear()

            for (ktExpression: KtExpression in function.referencesInBody) {
                if (usedThroughThisReference(ktExpression)) {
                    val resolved = ktExpression.resolveToElement ?: continue

                    if (resolved != function) {
                        if (isInvocationToExtractedFunction(resolved, extractedMethods) ||
                            isAccessToExtractedProperty(resolved, extractedFields)
                        ) {
                            distinctTargetDependencies.add(resolved)
                        } else {
                            distinctSourceDependencies.add(resolved)
                        }
                    }
                }

                if (pseudoUsedThroughThisReference(ktExpression)) {
                    val resolved = ktExpression.resolveToElement ?: continue

                    if (resolved != function) {
                        if (isInvocationToExtractedFunction(resolved, extractedMethods) ||
                            isAccessToExtractedProperty(resolved, extractedFields)
                        ) {
                            pseudoDistinctTargetDependencies.add(resolved)
                        } else {
                            pseudoDistinctSourceDependencies.add(resolved)
                        }
                    }
                }
            }

            pseudoDistinctSourceDependencies2 += pseudoDistinctSourceDependencies.size
            pseudoDistinctTargetDependencies2 += pseudoDistinctTargetDependencies.size
        }

        this.distinctTargetDependencies = distinctTargetDependencies.size
        this.distinctSourceDependencies = distinctSourceDependencies.size

        this.pseudoDistinctTargetDependencies = pseudoDistinctTargetDependencies2
        this.pseudoDistinctSourceDependencies = pseudoDistinctSourceDependencies2
    }

    private fun isInvocationToExtractedFunction(
        resolved: PsiElement?,
        extractedFunctions: Set<KtDeclaration>
    ): Boolean {
        for (method in extractedFunctions) {
            if (method == resolved) {
                return true
            }
        }

        return false
    }

    private fun isAccessToExtractedProperty(
        resolved: PsiElement?,
        extractedProperties: Set<KtDeclaration>
    ): Boolean {
        for (field in extractedProperties) {
            if (field == resolved) {
                return true
            }
        }
        return false
    }
}