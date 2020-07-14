package org.jetbrains.research.kotlincodesmelldetector.core;

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.research.kotlincodesmelldetector.utils.calculateCyclomaticComplexity
import org.jetbrains.research.kotlincodesmelldetector.utils.fields
import org.jetbrains.research.kotlincodesmelldetector.utils.isPropertyStoringValue
import org.jetbrains.research.kotlincodesmelldetector.utils.linesOfCode
import org.jetbrains.research.kotlincodesmelldetector.utils.methods
import org.jetbrains.research.kotlincodesmelldetector.utils.resolveToElement

private const val MINIMUM_NUMBER_OF_METHODS = 7
private const val MINIMUM_NUMBER_OF_FIELDS = 2
private const val WEIGHT_METHOD_COUNT_LOWER_BOUND = 50
private const val TIGHT_CLASS_COHESION_UPPER_BOUND = 0.33
private const val CLASS_LENGTH_TO_BE_CONSIDERED_AS_GOD_CLASS = 1000
private const val MINIMUM_NUMBER_OF_CLASSES_TO_FILTER = 4

private fun calculateAverageNumberOfMethodsPerClass(classes: List<SmartPsiElementPointer<KtElement>>): Double {
    var numberOfMethods = 0
    var numberOfClasses = 0

    for (classPointer in classes) {
        numberOfClasses++
        numberOfMethods += classPointer.element.methods.size
    }

    return numberOfMethods.toDouble() / numberOfClasses
}

private fun calculateAverageMethodLengthInProject(classes: List<SmartPsiElementPointer<KtElement>>): Double {
    var numberOfMethods = 0
    var loc = 0

    for (classPointer in classes) {
        val methods = classPointer.element.methods
        numberOfMethods += methods.size

        for (method in methods) {
            if (method is KtFunction) {
                loc += method.linesOfCode()
            } else if (method is KtProperty) {
                for (accessor in method.accessors) {
                    loc += accessor.linesOfCode()
                }
            }
        }
    }

    return loc.toDouble() / numberOfMethods
}

private fun collectAccessedFields(publicMethods: List<KtDeclaration>): Map<KtDeclaration, Set<KtDeclaration>> {
    fun acceptMethod(body: KtElement?, accessedFields: MutableSet<KtDeclaration>) {
        body?.accept(object : PsiElementVisitor() {
            override fun visitElement(psiElement: PsiElement) {
                run collect@{
                    val resolved = if (psiElement is KtExpression) {
                        psiElement.resolveToElement ?: return@collect
                    } else {
                        return@collect
                    }

                    if (resolved.containingFile != body.containingFile) { //TODO not just file
                        return@collect
                    }

                    val property = when (resolved) {
                        is KtPropertyAccessor -> {
                            resolved.property
                        }
                        is KtProperty -> {
                            resolved
                        }
                        else -> null
                    }

                    if (property?.isPropertyStoringValue == true) {
                        accessedFields.add(property)
                    }
                }

                for (child in psiElement.children) {
                    visitElement(child)
                }
            }
        })
    }

    val result = mutableMapOf<KtDeclaration, Set<KtDeclaration>>()

    for (method in publicMethods) {
        val accessedFields = mutableSetOf<KtDeclaration>()
        if (method is KtFunction) {
            acceptMethod(method.bodyExpression, accessedFields)
        } else if (method is KtProperty) {
            for (accessor in method.accessors) {
                acceptMethod(accessor.bodyExpression, accessedFields)
            }
        }

        result[method] = accessedFields
    }

    return result
}

private fun calculateTightClassCohesion(sourceClass: KtElement): Double {
    val publicMethods = sourceClass.methods.filter { it.isPublic }
    val membersUsed = collectAccessedFields(publicMethods)

    val numberOfPairs = publicMethods.size * (publicMethods.size - 1) / 2
    var connectedMethods = 0

    for (i in publicMethods.indices) {
        for (j in i + 1 until publicMethods.size) {
            val firstMembersUsed = membersUsed[publicMethods[i]] ?: mutableSetOf()
            val secondMembersUsed = membersUsed[publicMethods[j]] ?: mutableSetOf()
            if (firstMembersUsed.any { secondMembersUsed.contains(it) }) {
                connectedMethods++
            }
        }
    }

    return connectedMethods.toDouble() / numberOfPairs
}

private fun checkClassIsBigEnough(sourceClass: KtElement): Boolean {
    return sourceClass.methods.size >= MINIMUM_NUMBER_OF_METHODS && sourceClass.fields.size >= MINIMUM_NUMBER_OF_FIELDS
}

private fun checkWeightedMethodComplexity(
    sourceClass: KtElement
): Boolean {
    val methods = sourceClass.methods
    var weightedMethodComplexity = 0.0

    for (method in methods) {
        weightedMethodComplexity += calculateCyclomaticComplexity(method)
    }

    return weightedMethodComplexity >= WEIGHT_METHOD_COUNT_LOWER_BOUND
}

private fun checkTightClassCohesion(sourceClass: KtElement): Boolean {
    val tightClassCohesion = calculateTightClassCohesion(sourceClass)
    return tightClassCohesion < TIGHT_CLASS_COHESION_UPPER_BOUND
}

private fun checkClassIsTooBig(sourceClass: KtElement): Boolean {
    return sourceClass.linesOfCode() >= CLASS_LENGTH_TO_BE_CONSIDERED_AS_GOD_CLASS
}

private fun isGodClass(
    sourceClass: KtElement
): Boolean {
    return checkClassIsTooBig(sourceClass)
        || checkClassIsBigEnough(sourceClass)
        && checkWeightedMethodComplexity(sourceClass)
        && checkTightClassCohesion(sourceClass)
}

fun filterGodClasses(classes: List<SmartPsiElementPointer<KtElement>>): List<SmartPsiElementPointer<KtElement>> {
    if (classes.size < MINIMUM_NUMBER_OF_CLASSES_TO_FILTER) {
        return classes
    }

    val result = classes.filter { pointer ->
        val clazz = pointer.element
        clazz != null && isGodClass(clazz)
    }

    return result
}