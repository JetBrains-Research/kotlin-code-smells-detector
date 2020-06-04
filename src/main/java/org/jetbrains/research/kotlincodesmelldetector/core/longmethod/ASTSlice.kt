package org.jetbrains.research.kotlincodesmelldetector.core.longmethod

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.research.kotlincodesmelldetector.utils.getVariableDeclarationsAndParameters

class ASTSlice(sliceUnion: PDGSliceUnion, val project : Project) {
    val sourceMethodDeclaration: FirSimpleFunction = sliceUnion.function
    val sliceNodes: List<CFGNode<*>> = sliceUnion.sliceNodes
    val sliceStatements: MutableList<FirStatement> = mutableListOf()
    var localVariableCriterion: FirVariable<*>? = null
    init {
        for (node in sliceNodes) {
            if (node.fir is FirStatement) {
                sliceStatements.add(node.fir as FirStatement)
            }
        }

        // TODO handle accessed fields
        // Set<PsiVariable> variableDeclarationsAndAccessedFields = sliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod()
        val variableDeclarationsInFunction: List<FirVariable<*>> =  sliceUnion.function.getVariableDeclarationsAndParameters()
        val criterion = sliceUnion.localVariableCriterion
        for (variableDeclaration in variableDeclarationsInFunction) {
            if (variableDeclaration == criterion) {
                this.localVariableCriterion = variableDeclaration
                break
            }
        }
    }

    // TODO implement
    fun areSliceStatementsValid(astSlice: ASTSlice?): Boolean {
        return true
    }

    // TODO implement
    fun areSliceStatementsValid(): Boolean {
        return true
    }

    // TODO add enclosing class name if any
    override fun toString(): String {
        return sourceMethodDeclaration.name.toString()
    }
}