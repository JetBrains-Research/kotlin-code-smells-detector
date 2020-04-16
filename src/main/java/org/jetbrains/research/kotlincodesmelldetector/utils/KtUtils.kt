package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember

/**
 * If this is KtFile, returns all top-level functions and properties.
 * If this is KtClassOrObject, returns all declarations in class.
 * Otherwise, returns empty list.
 */
val KtElement.declarations: List<KtDeclaration>
    get() {
        if (this is KtClassOrObject) {
            return this.declarations
        } else if (this is KtFile) {
            return this.declarations.filter { ktDeclaration -> ktDeclaration is KtFunction || ktDeclaration is KtProperty }
        }

        return listOf()
    }

/**
 * @see KtElement.isMethod
 */
val KtElement?.methods: List<KtDeclaration>
    get() {
        val result = this?.declarations
            ?.filter { ktDeclaration -> ktDeclaration is KtFunction || ktDeclaration is KtProperty && !ktDeclaration.isField }
            ?.toMutableList()
            ?: mutableListOf()

        if (this is KtClassOrObject) {
            this.companionObjects.forEach { companionObject -> result.addAll(companionObject.methods) }
        }

        return result
    }

/**
 * @see KtElement.isField
 */
val KtElement?.fields: List<KtProperty>
    get() {

        return this?.declarations
            ?.filter { ktDeclaration -> ktDeclaration is KtProperty && ktDeclaration.isField }
            ?.map { ktDeclaration -> ktDeclaration as KtProperty }?.toMutableList() ?: mutableListOf()
    }

/**
 * "method" is either a KtFunction or KtProperty with overridden (non-trivial) getter or setter method, either a
 * top-level if this is KtFile, or the direct member of a class or object if this is a KtClassOrObject,
 * or the direct member of one of the class companion objects
 */
val KtElement.isMethod: Boolean
    get() {
        return this is KtFunction || this is KtProperty && !this.isField
    }

/**
 * "field" is a KtProperty without overridden (non-trivial) getter and setter method, top-level if this is KtFile,
 * or the direct member of a class or object if this is a KtClassOrObject,
 * or the direct member of one of the class companion objects
 */
val KtElement.isField: Boolean
    get() {
        if (this !is KtProperty) {
            return false
        }

        for (accessor in this.accessors) {
            if (accessor.hasBody()) {
                return false
            }
        }

        return true
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

    fun entityAccept(entity: KtElement, body: KtElement?) {
        body?.accept(object : PsiElementVisitor() {
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

    for (entity in entities) {
        if (entity is KtFunction) {
            entityAccept(entity, entity.bodyExpression)
        } else if (entity is KtProperty) {
            for (accessor in entity.accessors) {
                entityAccept(entity, accessor.bodyExpression)
            }
        }
    }

    return result
}

/**
 * Checks access to the same object, meaning both `this.field` and `field` are ok
 */
fun usedThroughThisReference(ktExpression: KtExpression): Boolean {
    //TODO test
    val resolvedElement = ktExpression.mainReference?.resolve() ?: return false
    if (resolvedElement !is KtElement || resolvedElement.classContext != ktExpression.classContext) {
        return false
    }

    val parent = ktExpression.parent

    return if (parent is KtDotQualifiedExpression) {
        parent.selectorExpression is KtThisExpression
    } else {
        true
    }
}

/**
 * Returns a class entity to which this element belongs, i.e. first non-local surrounding KtClass, KtObject or KtFile
 */
val KtElement.classContext: KtElement?
    get() {
        var parentElement = this.parent
        while (parentElement != null) {
            if (parentElement is KtClassOrObject && !parentElement.isLocal) {
                return parentElement
            } else if (parentElement is KtFile) {
                return parentElement
            }

            parentElement = parentElement.parent
        }

        return null
    }

/**
 * Returns list of KtExpression where each element is either KtCallExpression for function calls or
 * KtReferenceExpression for property accesses
 */
val KtDeclaration.referencesInBody: List<KtExpression>
    get() {
        val result = mutableListOf<KtExpression>()

        val visitor = object : PsiElementVisitor() {
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
        }

        if (this is KtFunction) {
            this.bodyExpression?.accept(visitor)
        } else if (this is KtProperty) {
            for (accessor in this.accessors) {
                accessor.bodyExpression?.accept(visitor)
            }
        }

        return result
    }

/**
 * Field consider to be static if defined inside companion object
 *
 * @see KtElement.isField
 */
val KtProperty.isStatic: Boolean
    get() {
        val parent = this.parent
        return parent is KtObjectDeclaration && parent.isCompanion()
    }