package org.jetbrains.research.kotlincodesmelldetector.core

import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.research.kotlincodesmelldetector.utils.referencesInBody
import org.jetbrains.research.kotlincodesmelldetector.utils.usedThroughThisReference

class GodClassVisualizationData(
    extractedMethods: Set<KtDeclaration>,
    extractedFields: Set<KtProperty>
) : VisualizationData {

    override val distinctTargetDependencies: Int

    override val distinctSourceDependencies: Int

    val containsNonAccessedPropertyInExtractedClass: Boolean
        get() {
            //TODO
            return false
        }

    init {
        var distinctTargetDependencies = 0
        var distinctSourceDependencies = 0

        for (function in extractedMethods) {
            for (ktExpression: KtExpression in function.referencesInBody) {
                if (usedThroughThisReference(ktExpression)) {
                    if (ktExpression is KtCallExpression && isInvocationToExtractedFunction(
                            ktExpression,
                            extractedMethods
                        ) ||
                        isAccessToExtractedProperty(ktExpression, extractedFields)
                    ) {
                        distinctTargetDependencies += 1
                    } else {
                        distinctSourceDependencies += 1
                    }
                }
            }
        }

        this.distinctTargetDependencies = distinctTargetDependencies
        this.distinctSourceDependencies = distinctSourceDependencies
    }

    private fun isInvocationToExtractedFunction(
        invocation: KtCallExpression,
        extractedFunctions: Set<KtDeclaration>
    ): Boolean {
        for (method in extractedFunctions) {
            if (method == invocation.mainReference.resolve()) {
                return true
            }
        }

        return false
    }

    private fun isAccessToExtractedProperty(
        access: KtExpression,
        extractedProperties: Set<KtProperty>
    ): Boolean {
        for (field in extractedProperties) {
            if (field == access.mainReference?.resolve()) {
                return true
            }
        }
        return false
    }
}