package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtVisitor

fun isUnused(ktProperty: KtProperty, ktClass: KtClassOrObject?): Boolean {
    //TODO
    return false
}

val KtClassOrObject?.functions: List<KtFunction>
    get() {
        return this?.declarations?.filterIsInstance<KtFunction>() ?: mutableListOf()
    }

val KtClassOrObject?.properties: List<KtProperty>
    get() {
        return this?.declarations?.filterIsInstance<KtProperty>() ?: mutableListOf()
    }

val KtProperty?.entitySet: List<KtElement>
    get() {
        val descriptor = this?.resolveToDescriptorIfAny() ?: return listOf()
        TODO("not implemented yet")
    }

fun <T : KtElement> T.toPointer(): SmartPsiElementPointer<T> {
    return SmartPointerManager.createPointer(this)
}

fun generateFullEntitySets(entities: List<KtElement>): Map<KtElement, Set<PsiElement>> {
    val result = HashMap<KtElement, MutableSet<PsiElement>>()
    for (entity in entities) {
        result[entity] = mutableSetOf()
        result[entity]?.add(entity)
    }

    for (entity in entities) {
        if (entity is KtFunction) {
            entity.accept(object : PsiElementVisitor() {
                override fun visitElement(psiElement: PsiElement) {
                    if (psiElement is KtCallExpression) {
                        val resolved = psiElement.mainReference.resolve()
                        resolved?.let { result[entity]?.add(it) }

                        if (resolved is KtPropertyAccessor) {
                            result[resolved]?.add(entity)
                        }
                    } else if (psiElement is KtReferenceExpression) {
                        val resolved = psiElement.resolve()
                        if (resolved is KtProperty) {
                            resolved.let { result[entity]?.add(it) }
                            result[resolved]?.add(entity)
                        }
                    }

                    for (child in psiElement.children) {
                        visitElement(child)
                    }
                }
            })
        }
    }

    return result
}

fun usedThroughThisReference(ktExpression: KtExpression): Boolean {
    val parent = ktExpression.parent
    return if (parent is KtDotQualifiedExpression) {
        parent.selectorExpression is KtThisExpression
    } else {
        true
    }
}

/**
 * Returns list of KtExpression where each element is either KtCallExpression for function calls or
 * KtReferenceExpression for property access
 */
val KtDeclarationWithBody.referencesInBody: List<KtExpression>
    get() {
        val result = mutableListOf<KtExpression>()

        this.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is KtCallExpression) {
                    result.add(element)
                } else if (element is KtReferenceExpression) {
                    if (element.mainReference.resolve() is KtProperty) {
                        result.add(element)
                    }
                }

                for (child in element.children) {
                    visitElement(child)
                }
            }
        })

        return result
    }