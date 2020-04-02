package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.research.kotlincodesmelldetector.core.GodClassVisualizationData
import org.jetbrains.research.kotlincodesmelldetector.utils.TopicFinder
import org.jetbrains.research.kotlincodesmelldetector.utils.functions
import org.jetbrains.research.kotlincodesmelldetector.utils.isUnused
import org.jetbrains.research.kotlincodesmelldetector.utils.properties
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.TreeMap

class ExtractClassCandidateRefactoring(
    private val projectInfo: ProjectInfo,
    val sourceClass: SmartPsiElementPointer<KtClassOrObject>,
    val extractedEntities: List<SmartPsiElementPointer<out KtElement>>
) : CandidateRefactoring(), Comparable<ExtractClassCandidateRefactoring> {

    private val leaveDelegate: Map<SmartPsiElementPointer<KtFunction>, Boolean>

    var defaultTargetClassName: String
        private set

    private val visualizationData: GodClassVisualizationData

    var topics: List<String>
        private set

    val extractedFunctions: Set<KtFunction>
        get() {
            val extractedFunctions: MutableSet<KtFunction> =
                LinkedHashSet()
            for (entity in extractedEntities) {
                if (entity.element is KtFunction) {
                    extractedFunctions.add(entity.element as KtFunction)
                }
            }

            return extractedFunctions
        }

    val delegateFunctions: Set<KtFunction>
        get() {
            val delegateFunctions: MutableSet<KtFunction> =
                LinkedHashSet()
            for (method in leaveDelegate.keys) {
                if (leaveDelegate[method] == true) {
                    delegateFunctions.add(method.element!!)
                }
            }

            return delegateFunctions
        }

    val extractedProperties: Set<KtProperty>
        get() {
            val extractedFieldFragmentMap: MutableMap<Int, KtProperty> =
                TreeMap()

            val declarations = sourceClass.element?.declarations ?: mutableListOf()
            for (entity in extractedEntities) {
                if (entity.element is KtProperty) {
                    val index: Int = declarations.indexOf(entity.element!!)
                    extractedFieldFragmentMap[index] = entity.element as KtProperty
                }
            }

            return LinkedHashSet(extractedFieldFragmentMap.values)
        }

    val isApplicable: Boolean
        get() {
            var methodCounter = 0
            for (entity in extractedEntities) {
                if (entity.element is KtFunction) {
                    val method = entity.element as KtFunction
                    methodCounter++

                    /*
                    TODO review and realise relevant for Kotlin checks

                    if (isSynchronized(method) || containsSuperMethodInvocation(method) ||
                        overridesMethod(method) || method.isAbstract() || containsFieldAccessOfEnclosingClass(method) ||
                        isReadObject(method) || isWriteObject(method)
                    ) {
                        return false
                    }

                     */
                } else if (entity.element is KtProperty) {
                    val property = entity.element as KtProperty
                    if (property.hasModifier(KtModifierKeywordToken.keywordModifier("private"))) {
                        if (isUnused(property, sourceClass.element)) {
                            return false
                        }
                    }
                }
            }

            return (extractedEntities.size > 2 && methodCounter != 0 && validRemainingMethodsInSourceClass()
                && validRemainingFieldsInSourceClass() && !visualizationData.containsNonAccessedPropertyInExtractedClass)
        }

    private fun validRemainingMethodsInSourceClass(): Boolean {
        for (sourceMethod in sourceClass.element.functions) {
            if (extractedEntities.find { entity -> entity.element == sourceMethod } == null) {
                if (!sourceMethod.isAbstract()
                /*
                TODO review and realise relevant for Kotlin checks here

                !sourceMethod.isStatic && sourceMethod.isDelegate == null &&
                !sourceMethod.isReadObject && !sourceMethod.isWriteObject && !sourceMethod.isEquals
                && !sourceMethod.isHashCode && !sourceMethod.isClone && !sourceMethod.isCompareTo
                && !sourceMethod.isToString
                 */
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun validRemainingFieldsInSourceClass(): Boolean {
        for (sourceProperty in sourceClass.element.properties) {
            if (extractedEntities.find { entity -> entity.element == sourceProperty } == null) {
                return true
            }
        }

        return false
    }

    override fun getTarget(): SmartPsiElementPointer<KtClassOrObject>? {
        return null
    }

    override fun getSource(): SmartPsiElementPointer<KtClassOrObject> {
        return sourceClass
    }

    override fun toString(): String {
        return sourceClass.element?.name.toString() + "\t" + extractedEntities.toString()
    }

    override fun compareTo(other: ExtractClassCandidateRefactoring): Int {
        val thisSourceClassDependencies = this.distinctSourceDependencies
        val otherSourceClassDependencies = other.distinctSourceDependencies
        return if (thisSourceClassDependencies != otherSourceClassDependencies) {
            thisSourceClassDependencies.compareTo(otherSourceClassDependencies)
        } else {
            val thisTargetClassDependencies = this.distinctTargetDependencies
            val otherTargetClassDependencies = other.distinctTargetDependencies
            if (thisTargetClassDependencies != otherTargetClassDependencies) {
                -thisTargetClassDependencies.compareTo(otherTargetClassDependencies)
            } else {
                sourceClass.element?.name!!.compareTo(other.sourceClass.element?.name!!)
            }
        }
    }

    fun findTopics() {
        val codeElements: MutableList<String> = ArrayList()
        for (entity in extractedEntities) {
            if (entity.element is KtNamedDeclaration) {
                codeElements.add(entity.element?.name ?: "")
            }
        }

        topics = TopicFinder.findTopics(codeElements)
    }

    override fun getDistinctSourceDependencies(): Int {
        return visualizationData.distinctSourceDependencies
    }

    override fun getDistinctTargetDependencies(): Int {
        return visualizationData.distinctTargetDependencies
    }

    override fun getSourceEntity(): SmartPsiElementPointer<KtClassOrObject> {
        return sourceClass
    }

    val sourceFile: KtFile?
        get() = sourceClass.element?.containingKtFile

    init {
        leaveDelegate = LinkedHashMap()
        defaultTargetClassName = sourceClass.element!!.name + "Product"
        for (ktClass in projectInfo.classes) {
            if (ktClass.element!!.name == defaultTargetClassName) {
                defaultTargetClassName += "2"
                break
            }
        }
        topics = ArrayList()
        val extractedFunctions: MutableSet<SmartPsiElementPointer<out KtFunction>> =
            LinkedHashSet()
        val extractedProperties: MutableSet<SmartPsiElementPointer<out KtProperty>> =
            LinkedHashSet()

        for (entity in extractedEntities) {
            if (entity.element is KtFunction) {
                extractedFunctions.add(entity as SmartPsiElementPointer<out KtFunction>)
            } else if (entity.element is KtProperty) {
                extractedProperties.add(entity as SmartPsiElementPointer<out KtProperty>)
            }
        }

        visualizationData = GodClassVisualizationData(
            sourceClass,
            this.extractedFunctions, this.extractedProperties
        )
    }
}