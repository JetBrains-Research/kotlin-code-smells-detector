package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.research.kotlincodesmelldetector.core.FeatureEnvyVisualizationData
import org.jetbrains.research.kotlincodesmelldetector.utils.*

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
        return  !targetClass.isInterface && validTargetObject() && !targetClassContainsMethodWithSourceMethodSignature()
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
            if (getConstructorType(variable) == targetClass.signature) {
                return false
            }
        }
        val candidateReferences = mutableListOf<KtNamedDeclaration>()
        candidateReferences.addAll(sourceMethod.valueParameters)
        candidateReferences.addAll(sourceClass.attributeList)
        for (candidate in candidateReferences) {
            if (getConstructorType(candidate) == targetClass.signature && candidate.type()?.isMarkedNullable == false) {
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
        val targetClassType = targetClass.signature
        for (variable in accessedVariables) {
            val type = getConstructorType(variable)
            type?.let {
                if (isContainer(type) && getFirstTypeArgumentType(variable) == targetClassType) {
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
        val sourceMethodSignature = shortMethodName(sourceMethod)
        for (targetMethod in targetClass.methodList) {
            if (sourceMethodSignature == shortMethodName(targetMethod)) {
                return true
            }
        }
        return false
    }

    private fun shortMethodName(method: KtNamedFunction) = method.signature?.substringBeforeLast("(")?.substringAfterLast(".")

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
