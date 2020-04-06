package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class MoveMethodCandidateRefactoring(val project: ProjectInfo, val sourceClass: ClassEntity, val targetClass: ClassEntity, val sourceMethod: KtNamedFunction) : CandidateRefactoring(), Comparable<MoveMethodCandidateRefactoring> {
    var intersectionWithTargetClass: Int = 0
    var intersectionWithSourceClass: Int = 0
    override fun getDistinctSourceDependencies(): Int {
        TODO("Not yet implemented")
    }

    override fun getTarget(): SmartPsiElementPointer<KtClassOrObject> {
        TODO("Not yet implemented")
    }

    override fun getSource(): SmartPsiElementPointer<KtClassOrObject> {
        TODO("Not yet implemented")
    }

    override fun getDistinctTargetDependencies(): Int {
        TODO("Not yet implemented")
    }

    override fun getSourceEntity(): SmartPsiElementPointer<KtClassOrObject> {
        TODO("Not yet implemented")
    }

    override fun compareTo(other: MoveMethodCandidateRefactoring): Int {
        if (intersectionWithTargetClass != other.intersectionWithTargetClass)
            return other.intersectionWithTargetClass - intersectionWithTargetClass
        return intersectionWithSourceClass - other.intersectionWithSourceClass
    }

    fun isApplicable(): Boolean {
        if (targetClass.element is KtObjectDeclaration) {
            return true
        }
        for (parameter in sourceMethod.valueParameters) {
            if (parameter.type()?.constructor?.declarationDescriptor?.fqNameOrNull() == targetClass.element.fqName) {
                return true
            }
        }
        for (attribute in sourceClass.attributeList) {
            if (attribute.isPublic && attribute.type()?.constructor?.declarationDescriptor?.fqNameOrNull() == targetClass.element.fqName) {
                return true
            }
        }
        return false
    }
}
