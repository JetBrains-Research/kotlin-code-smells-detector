package org.jetbrains.research.kotlincodesmelldetector.utils

import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration

private fun KtDeclaration.hasAnnotation(fqName: String): Boolean {
    return this.findAnnotation(FqName(fqName)) != null
}

val KtDeclaration.isSynchronized: Boolean
    get() {
        return this.hasAnnotation("Synchronized")
    }

val KtDeclaration.isToString: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isCompareTo: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isClone: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isHashCode: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isEquals: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isDelegate: KtDeclaration?
    get() {
        //TODO
        return null
    }

val KtDeclaration.isStatic: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isWriteObject: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isReadObject: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.containsFieldAccessOfEnclosingClass: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.overridesMethod: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.containsSuperMethodInvocation: Boolean
    get() {
        //TODO
        return false
    }

val KtDeclaration.isAbstract: Boolean
    get() {
        //TODO
        return false
    }