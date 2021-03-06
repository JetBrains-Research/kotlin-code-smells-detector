package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

/**
 * If this is KtFile, returns all top-level functions and properties.
 * If this is KtClassOrObject, returns all (top level) declarations in class, including val/var declared at primary constructor.
 * Otherwise, returns empty list.
 */
val KtElement.declaredElements: List<KtDeclaration>
    get() {
        return when (this) {
            is KtClassOrObject -> {
                val result = this.declarations.toMutableList()
                result.addAll(this.primaryConstructorParameters.filter { parameter -> parameter.isPropertyParameter() })
                result
            }
            is KtFile -> {
                this.declarations.filter { ktDeclaration -> ktDeclaration is KtFunction || ktDeclaration is KtProperty }
            }
            else -> {
                listOf()
            }
        }
    }

/**
 * @see KtElement.isMethod
 */
val KtElement?.methods: List<KtDeclaration>
    get() {
        val result = this?.declaredElements
            ?.filter { ktDeclaration -> ktDeclaration.isMethod }
            ?.filter { ktDeclaration -> ktDeclaration.correctMethod }
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
val KtElement?.fields: List<KtDeclaration>
    get() {
        return this?.declaredElements
            ?.filter { ktDeclaration -> ktDeclaration.isField }
            ?: listOf()
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
 * TODO
 */
private val KtDeclaration.correctMethod: Boolean
    get() {
        if (this.isDelegate) {
            return false
        }

        return true
    }

/**
 * "field" is a KtProperty without overridden (non-trivial) getter and setter method, top-level if this is KtFile,
 * or the direct member of a class or object if this is a KtClassOrObject (including primary constructor parameter),
 * or the direct member of one of the class companion objects
 */
val KtElement.isField: Boolean
    get() {
        if (this is KtParameter && this.hasValOrVar()) {
            return true
        }

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

val KtElement.isPropertyOrConstructorVar: Boolean
    get() {
        return this is KtProperty || this is KtParameter && this.isPropertyParameter()
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
                    val resolved = psiElement.resolveToElement

                    if (resolved !is KtClassOrObject && resolved !is KtPrimaryConstructor && resolved !is KtSecondaryConstructor) { //TODO review
                        resolved?.let { result[entity]?.add(it) }

                        if (resolved is KtPropertyAccessor && resolved.property.isField) {
                            result[resolved]?.add(entity)
                        }
                    }
                } else if (psiElement is KtReferenceExpression) {
                    val resolved = psiElement.resolveToElement
                    if (resolved is KtElement && resolved.isField) {
                        result[entity]?.add(resolved)
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

val KtExpression.resolveToElement: PsiElement?
    get() {
        return when (this) {
            is KtCallExpression -> {
                calleeExpression?.mainReference?.resolve()
            }

            is KtReferenceExpression -> {
                mainReference.resolve()
            }

            else -> {
                null
            }
        }
    }

/**
 * Checks access to the same object, meaning both `this.field` and `field` are ok
 */
fun usedThroughThisReference(ktExpression: KtExpression): Boolean {
    //TODO test
    val resolvedElement = ktExpression.resolveToElement

    return when {
        resolvedElement !is KtFunction && resolvedElement !is KtProperty && resolvedElement !is KtParameter -> {
            false
        }

        resolvedElement is KtFunction && resolvedElement.isLocal
            || resolvedElement is KtProperty && resolvedElement.isLocal
            || resolvedElement is KtParameter && !resolvedElement.hasValOrVar() -> {
            false
        }

        resolvedElement !is KtElement || resolvedElement.classContext != ktExpression.classContext -> {
            false
        }

        else -> {
            val parent = ktExpression.parent

            return if (parent is KtDotQualifiedExpression) {
                parent.selectorExpression == ktExpression || parent.selectorExpression is KtThisExpression
            } else {
                true
            }
        }
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
                    val resolved = element.resolveToElement
                    if (resolved is KtElement && resolved.isPropertyOrConstructorVar) {
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

val KtNamedDeclaration.signature: String?
    get() {
        val fqName = this.fqName?.asString() ?: return null
        if (this is KtNamedFunction) {
            val parameters = this.valueParameters
                .map { it.type()?.toString() }
                .joinToString(", ", "(", ")")
            return fqName + parameters
        }
        return fqName
    }

val acceptableOriginClassNames = setOf(
    "kotlin.collections.Collection", "kotlin.collections.MutableCollection",
    "kotlin.collections.AbstractCollection", "kotlin.collections.List", "kotlin.collections.MutableList",
    "kotlin.collections.AbstractList", "kotlin.collections.ArrayList", "java.util.LinkedList",
    "kotlin.collections.Set", "kotlin.collections.MutableSet", "java.util.AbstractSet", "java.util.HashSet",
    "java.util.LinkedHashSet", "java.util.SortedSet", "java.util.TreeSet", "java.util.Vector", "java.util.Stack"
)

fun isContainer(type: FqName): Boolean {
    return type.asString() in acceptableOriginClassNames
}

fun getConstructorType(declaration: KtNamedDeclaration): FqName? {
    return declaration.type()?.fqName
}

fun getFirstTypeArgumentType(declaration: KtNamedDeclaration): FqName? {
    return declaration.type()?.arguments?.getOrNull(0)?.type?.constructor?.declarationDescriptor?.fqNameOrNull()
}