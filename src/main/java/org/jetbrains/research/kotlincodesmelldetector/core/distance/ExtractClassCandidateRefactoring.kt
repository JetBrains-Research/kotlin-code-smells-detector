package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.util.hasPrivateModifier
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.research.kotlincodesmelldetector.core.GodClassVisualizationData
import org.jetbrains.research.kotlincodesmelldetector.utils.TopicFinder
import org.jetbrains.research.kotlincodesmelldetector.utils.containsFieldAccessOfEnclosingClass
import org.jetbrains.research.kotlincodesmelldetector.utils.containsSuperMethodInvocation
import org.jetbrains.research.kotlincodesmelldetector.utils.declaredElements
import org.jetbrains.research.kotlincodesmelldetector.utils.isAbstract
import org.jetbrains.research.kotlincodesmelldetector.utils.isDelegate
import org.jetbrains.research.kotlincodesmelldetector.utils.isField
import org.jetbrains.research.kotlincodesmelldetector.utils.isMethod
import org.jetbrains.research.kotlincodesmelldetector.utils.isSynchronized
import org.jetbrains.research.kotlincodesmelldetector.utils.methods
import org.jetbrains.research.kotlincodesmelldetector.utils.overridesMethod
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.TreeMap

class ExtractClassCandidateRefactoring(
    projectInfo: ProjectInfo,
    val sourceClass: SmartPsiElementPointer<KtElement>,
    val extractedEntities: List<SmartPsiElementPointer<out KtDeclaration>>
) : CandidateRefactoring(), Comparable<ExtractClassCandidateRefactoring> {

    private val leaveDelegate: Map<SmartPsiElementPointer<out KtDeclaration>, Boolean>

    var defaultTargetClassName: String
        private set

    private val visualizationData: GodClassVisualizationData

    var topics: List<String>
        private set

    val extractedMethods: Set<KtDeclaration>
        get() {
            val extractedFunctions: MutableSet<KtDeclaration> =
                LinkedHashSet()

            for (entity in extractedEntities) {
                val element = entity.element
                if (element?.isMethod == true) {
                    extractedFunctions.add(element)
                }
            }

            return extractedFunctions
        }

    val delegateFunctions: Set<KtDeclaration>
        get() {
            val delegateFunctions: MutableSet<KtDeclaration> =
                LinkedHashSet()

            for (method in leaveDelegate.keys) {
                val element = method.element
                if (leaveDelegate[method] == true && element != null) {
                    delegateFunctions.add(element)
                }
            }

            return delegateFunctions
        }

    val extractedFields: Set<KtDeclaration>
        get() {
            val extractedFieldFragmentMap: MutableMap<Int, KtDeclaration> =
                TreeMap()

            val declarations = sourceClass.element?.declaredElements ?: mutableListOf()
            for (entity in extractedEntities) {
                val element = entity.element
                if (element?.isField == true) {
                    val index = declarations.indexOf(element)
                    extractedFieldFragmentMap[index] = element
                }
            }

            return LinkedHashSet(extractedFieldFragmentMap.values)
        }

    val isApplicable: Boolean
        get() {
            var methodCounter = 0
            for (entity in extractedEntities) {
                val element = entity.element
                if (element?.isField == true) {
                    if (!element.hasPrivateModifier()) {
                        return false
                    }
                } else {
                    methodCounter++

                    if (element is KtProperty) {
                        for (accessor in element.accessors) {
                            if (accessor.methodNotExtractable()) {
                                return false
                            }
                        }
                    } else if (element is KtFunction) {
                        if (element.methodNotExtractable()) {
                            return false
                        }
                    }
                }
            }

            return extractedEntities.size > 2 && methodCounter != 0 && validRemainingMethodsInSourceClass()
                && !visualizationData.containsNonAccessedPropertyInExtractedClass
        }

    private fun KtDeclaration.methodNotExtractable(): Boolean {
        return this.isSynchronized || this.containsSuperMethodInvocation ||
            this.overridesMethod || this.isAbstract || this.containsFieldAccessOfEnclosingClass
    }

    private fun KtDeclaration.sourceMethodValid(): Boolean {
        return !this.isAbstract && this.isDelegate == null
    }

    private fun validRemainingMethodsInSourceClass(): Boolean {
        for (sourceMethod in sourceClass.element.methods) {
            if (extractedEntities.find { entity -> entity.element == sourceMethod } == null) {
                if (sourceMethod is KtProperty) {
                    if (sourceMethod.accessors.all { accessor -> accessor.sourceMethodValid() }) {
                        return true
                    }
                } else if (sourceMethod is KtFunction) {
                    if (sourceMethod.sourceMethodValid()) {
                        return true
                    }
                }
            }
        }

        return false
    }

    override fun getTarget(): SmartPsiElementPointer<KtClassOrObject>? {
        return null
    }

    override fun getSource(): SmartPsiElementPointer<KtElement> {
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

    override fun getSourceEntity(): SmartPsiElementPointer<KtElement> {
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

        visualizationData = GodClassVisualizationData(
            extractedMethods, extractedFields
        )
    }
}
