package org.jetbrains.research.kotlincodesmelldetector.core.distance

import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

data class ClassEntity(val element: KtClassOrObject) {
    val isEnum = element is KtClass && element.isEnum()
    val isInterface = element is KtClass && element.isInterface()
    val attributeList: MutableList<KtNamedDeclaration> = mutableListOf()
    val methodList: MutableList<KtNamedFunction> = mutableListOf()

    init {
        val members = element.resolveToDescriptorIfAny()?.unsubstitutedMemberScope
        members?.let {
            for (variable in it.getDescriptorsFiltered(DescriptorKindFilter.VARIABLES)) {
                variable.findPsi()?.let { psiElement ->
                    if (psiElement is KtProperty || (psiElement is KtParameter && psiElement.hasValOrVar())) {
                        attributeList.add(psiElement as KtNamedDeclaration)
                    }
                }
            }
            for (function in it.getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS)) {
                function.findPsi()?.let { psiElement ->
                    if (psiElement is KtNamedFunction) {
                        methodList.add(psiElement)
                    }
                }
            }
        }
    }
}
