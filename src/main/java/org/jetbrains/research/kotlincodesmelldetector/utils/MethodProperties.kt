package org.jetbrains.research.kotlincodesmelldetector.utils

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration

private fun KtDeclaration.hasAnnotation(shortName: String): Boolean {
    return this.annotationEntries.any { annotation -> annotation.shortName?.asString() == shortName }
}

val KtDeclaration.isSynchronized: Boolean
    get() {
        return hasAnnotation("Synchronized")
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
        return this.hasModifier(KtTokens.OVERRIDE_KEYWORD)
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