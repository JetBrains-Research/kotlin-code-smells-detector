package org.jetbrains.research.kotlincodesmelldetector.core

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.research.kotlincodesmelldetector.utils.referencesInBody
import org.jetbrains.research.kotlincodesmelldetector.utils.usedThroughThisReference

class GodClassVisualizationData(
    private val sourceClass: SmartPsiElementPointer<KtClassOrObject>,
    private val extractedFunctions: Set<KtFunction>,
    private val extractedProperties: Set<KtProperty>
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

        for (function in extractedFunctions) {
            for (ktExpression: KtExpression in function.referencesInBody) {
                if (usedThroughThisReference(ktExpression)) {
                    if (ktExpression is KtCallExpression && isInvocationToExtractedFunction(ktExpression) ||
                        isAccessToExtractedProperty(ktExpression)
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
        invocation: KtCallExpression
    ): Boolean {
        for (method in extractedFunctions) {
            if (method == invocation.mainReference.resolve()) {
                return true
            }
        }

        return false
    }

    private fun isAccessToExtractedProperty(
        access: KtExpression
    ): Boolean {
        for (field in extractedProperties) {
            if (field == access.mainReference?.resolve()) {
                return true
            }
        }
        return false
    }
}