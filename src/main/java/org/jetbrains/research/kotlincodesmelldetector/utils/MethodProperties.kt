package org.jetbrains.research.kotlincodesmelldetector.utils

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReturnExpression

private fun KtDeclaration.hasAnnotation(shortName: String): Boolean {
    return this.annotationEntries.any { annotation -> annotation.shortName?.asString() == shortName }
}

val KtDeclaration.isSynchronized: Boolean
    get() {
        return hasAnnotation("Synchronized")
    }

val KtDeclaration.isOpen: Boolean
    get() {
        return hasModifier(KtTokens.OPEN_KEYWORD)
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

val KtDeclaration.isDelegate: Boolean
    get() {
        if (this !is KtDeclarationWithBody) {
            return false
        }

        val statement =
            if (this.hasBlockBody()) {
                val statements = this.bodyBlockExpression?.statements ?: return false
                if (statements.size != 1) {
                    return false
                } else {
                    statements[0]
                }
            } else if (this.hasBody()) {
                this.bodyExpression ?: return false
            } else {
                return false
            }

        val methodInvocation: KtExpression =
            (if (statement is KtReturnExpression) {
                statement.returnedExpression
            } else {
                statement
            })
                ?: return false

        return methodInvocation is KtCallExpression && usedThroughThisReference(methodInvocation)
    }