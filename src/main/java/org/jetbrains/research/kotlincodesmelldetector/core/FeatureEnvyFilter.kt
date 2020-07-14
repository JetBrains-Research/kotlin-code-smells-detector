package org.jetbrains.research.kotlincodesmelldetector.core;

import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ClassEntity

fun isFeatureEnvyCandidate(
    method: KtFunction,
    referencesInBody: List<KtNameReferenceExpression>,
    classes: MutableMap<KtClassOrObject, ClassEntity>
): Boolean {
    val classesAccessed = mutableSetOf<KtElement>()

    val sourceMembersAccessed = mutableSetOf<KtElement>()
    val envyMembersAccessed = mutableSetOf<KtElement>()

    for (reference in referencesInBody) {
        reference.mainReference.resolve()?.let { called ->
            if (called is KtNamedDeclaration) {
                val containingClass = called.containingClassOrObject
                if (containingClass != null && containingClass in classes.keys) {
                    classesAccessed.add(containingClass)

                    if (called is KtProperty || (called is KtParameter && called.hasValOrVar())) {
                        if (containingClass == method.containingClassOrObject) {
                            sourceMembersAccessed.add(called)
                        } else {
                            envyMembersAccessed.add(called)
                        }
                    }
                }
            }
        }
    }

    return checkATFD(envyMembersAccessed) && checkLAA(sourceMembersAccessed, envyMembersAccessed) && checkFDP(classesAccessed)
}

private fun checkATFD(envyMembersAccessed: MutableSet<KtElement>): Boolean {
    return envyMembersAccessed.size > 2
}

private fun checkLAA(
    sourceMembersAccessed: MutableSet<KtElement>,
    envyMembersAccessed: MutableSet<KtElement>
): Boolean {
    val source = sourceMembersAccessed.size
    val envy = envyMembersAccessed.size
    return if (source == 0 && envy == 0) {
        false
    } else {
        source.toDouble() / (source + envy) < 0.33
    }
}

private fun checkFDP(classesAccessed: MutableSet<KtElement>): Boolean {
    return classesAccessed.size <= 3 //TODO constant
}