package org.jetbrains.research.kotlincodesmelldetector.utils

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

private val ConditionalTokens = setOf(KtTokens.ANDAND, KtTokens.OROR, KtTokens.ELVIS)

private val IfSubstitutes = setOf(
    "run",
    "runCatching",
    "let",
    "with",
    "apply",
    "also",
    "use",
    "forEach",
    "takeIf",
    "takeUnless",
    "isNotNull",
    "ifNull"
)

fun calculateCyclomaticComplexity(node: KtElement): Int {
    val visitor = CyclomaticComplexity()
    node.accept(visitor)
    return visitor.complexity
}

class CyclomaticComplexity : KtTreeVisitorVoid() {
    var complexity: Int = 0
        private set

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        if (expression.operationToken in ConditionalTokens) {
            complexity++
        }

        super.visitBinaryExpression(expression)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        complexity++
        super.visitIfExpression(expression)
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
        complexity++
        super.visitLoopExpression(loopExpression)
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        complexity += expression.entries.count()
        super.visitWhenExpression(expression)
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        complexity += expression.catchClauses.size
        super.visitTryExpression(expression)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        if (IfSubstitutes.contains(expression.calleeExpression?.text)) {
            complexity++
        }

        super.visitCallExpression(expression)
    }
}
