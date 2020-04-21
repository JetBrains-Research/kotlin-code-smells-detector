package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.lang.jvm.actions.constructorRequest
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.research.kotlincodesmelldetector.core.FeatureEnvyVisualizationData
import org.jetbrains.research.kotlincodesmelldetector.utils.*
import kotlin.reflect.jvm.internal.impl.name.FqName

class MoveMethodCandidateRefactoring(val project: ProjectInfo, private val sourceClass: ClassEntity, val targetClass: ClassEntity, val sourceMethod: KtNamedFunction) : CandidateRefactoring(), Comparable<MoveMethodCandidateRefactoring> {
    val visualizationData: FeatureEnvyVisualizationData by lazy {
        FeatureEnvyVisualizationData(sourceClass, sourceMethod, targetClass)
    }

    override fun getDistinctSourceDependencies(): Int {
        return visualizationData.distinctSourceDependencies
    }

    override fun getDistinctTargetDependencies(): Int {
        return visualizationData.distinctTargetDependencies
    }


    override fun compareTo(other: MoveMethodCandidateRefactoring): Int {
        if (distinctSourceDependencies != other.distinctSourceDependencies)
            return distinctSourceDependencies - other.distinctSourceDependencies
        return other.distinctTargetDependencies - distinctTargetDependencies

    }

    fun isApplicable(): Boolean {
        return !sourceMethod.isSynchronized && !sourceMethod.overridesMethod && !targetClass.isInterface
                && validTargetObject() && !targetClassContainsMethodWithSourceMethodSignature()
                && !isSourceClassATestClass() && !containsFieldAssignment() && !containsSuperInvocation()
                && !oneToManyRelationshipWithTargetClass()
    }

    private fun containsFieldAssignment(): Boolean {
        return visualizationData.sourceAssignments > 0
    }

    private fun containsSuperInvocation(): Boolean {
        return visualizationData.containsSuperInvocation
    }

    private fun validTargetObject(): Boolean {
        if (targetClass.element is KtObjectDeclaration) {
            return true
        }
        val localVariables = sourceMethod.bodyExpression?.collectDescendantsOfType<KtProperty>() ?: emptyList()
        for (variable in localVariables) {
            if (variable.type()?.constructor?.declarationDescriptor?.fqNameOrNull() == targetClass.fqName) {
                return false
            }
        }
        val candidateReferences = mutableListOf<KtNamedDeclaration>()
        candidateReferences.addAll(sourceMethod.valueParameters)
        candidateReferences.addAll(sourceClass.attributeList)
        for (candidate in candidateReferences) {
            if (candidate.type()?.constructor?.declarationDescriptor?.fqNameOrNull() == targetClass.fqName) {
                return true
            }
        }
        return false
    }

    private fun oneToManyRelationshipWithTargetClass(): Boolean {
        val accessedVariables = mutableListOf<KtNamedDeclaration>()
        accessedVariables.addAll(sourceMethod.bodyExpression?.collectDescendantsOfType<KtProperty>() ?: emptyList())
        accessedVariables.addAll(sourceMethod.valueParameters)
        accessedVariables.addAll(visualizationData.sourceAccessedMembers.filter { member -> member is KtParameter || member is KtProperty })
        val targetClassType = targetClass.fqName
        for (variable in accessedVariables) {
            val type = getConstructorType(variable)
            type?.let {
                if (isContainer(type) && getGenericType(variable) == targetClassType) {
                    return true
                }
            }
        }
        return false
    }

    private fun isSourceClassATestClass(): Boolean {
        return isTestClass(sourceClass.element)
    }

    private fun targetClassContainsMethodWithSourceMethodSignature(): Boolean {
        val methodSignature = sourceMethod.nameWithParameterList.shortName()
        for (method in targetClass.methodList) {
            if (methodSignature == method.nameWithParameterList.shortName()) {
                return true
            }
        }
        return false
    }


    override fun getTarget(): SmartPsiElementPointer<KtClassOrObject> {
        TODO("Not yet implemented")
    }

    override fun getSource(): SmartPsiElementPointer<KtElement> {
        TODO("Not yet implemented")
    }

    override fun getSourceEntity(): SmartPsiElementPointer<KtElement> {
        TODO("Not yet implemented")
    }
}
