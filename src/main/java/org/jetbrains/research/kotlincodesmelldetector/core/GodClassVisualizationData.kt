package org.jetbrains.research.kotlincodesmelldetector.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.research.kotlincodesmelldetector.utils.referencesInBody
import org.jetbrains.research.kotlincodesmelldetector.utils.resolveToElement
import org.jetbrains.research.kotlincodesmelldetector.utils.usedThroughThisReference

class GodClassVisualizationData(
    extractedMethods: Set<KtDeclaration>,
    extractedFields: Set<KtDeclaration>
) : VisualizationData {

    override val distinctTargetDependencies: Int

    override val distinctSourceDependencies: Int

    init {
        val distinctTargetDependencies = mutableSetOf<PsiElement>()
        val distinctSourceDependencies = mutableSetOf<PsiElement>()

        for (function in extractedMethods) {
            for (ktExpression: KtExpression in function.referencesInBody) {
                if (usedThroughThisReference(ktExpression)) {
                    val resolved = ktExpression.resolveToElement ?: continue

                    if (isInvocationToExtractedFunction(resolved, extractedMethods) ||
                        isAccessToExtractedProperty(resolved, extractedFields)
                    ) {
                        distinctTargetDependencies.add(resolved)
                    } else {
                        distinctSourceDependencies.add(resolved)
                    }
                }
            }
        }

        this.distinctTargetDependencies = distinctTargetDependencies.size
        this.distinctSourceDependencies = distinctSourceDependencies.size
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