package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.analysis.cfa.TraverseDirection
import org.jetbrains.kotlin.fir.analysis.cfa.traverse
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.isNotEmpty
import org.jetbrains.kotlin.fir.resolve.dfa.stackOf
import org.jetbrains.kotlin.idea.fir.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.getOrBuildFir

fun extractFirSimpleFunctions(firFile: FirFile): List<FirSimpleFunction> {
    val result = mutableListOf<FirSimpleFunction>()
    result.addAll(firFile.declarations.filterIsInstance<FirSimpleFunction>())
    // TODO other types of classes
    result.addAll(firFile.declarations.filterIsInstance<FirRegularClass>().flatMap {
        it.declarations.filterIsInstance<FirSimpleFunction>()
    })
    return result
}

fun getCurrentFirFileOpenInEditor(project: Project): FirFile {
    val ktFile = getCurrentFileOpenInEditor(project)
    val firFile = ktFile?.getOrBuildFir(FirModuleResolveStateImpl(FirProjectSessionProvider(project)), FirResolvePhase.BODY_RESOLVE)!!
    return firFile
}

/**
 * Returns variables inside as well as function parameters
 */
fun getVariableDeclarationsAndParameters(firSimpleFunction: FirSimpleFunction, cfg: ControlFlowGraph): List<FirVariable<*>> {
    val result = mutableListOf<FirVariable<*>>()
    result.addAll(firSimpleFunction.valueParameters)
    // TODO better way to find all vars and vals
    result.addAll(cfg.nodes.map { it.fir }.filterIsInstance<FirVariable<*>>().filter { !it.name.isSpecial })
    return result
}

/**
 * Returns variables inside as well as function parameters
 */
fun FirSimpleFunction.getVariableDeclarationsAndParameters() : List<FirVariable<*>> {
    if (this.controlFlowGraphReference !is FirControlFlowGraphReferenceImpl) {
        return emptyList()
    }
    val cfg = (this.controlFlowGraphReference as FirControlFlowGraphReferenceImpl).controlFlowGraph
    return getVariableDeclarationsAndParameters(this, cfg)
}

/**
 * Returns variables declarations inside function
 */
fun FirSimpleFunction.getVariableDeclarations() : List<FirVariable<*>> {
    if (this.controlFlowGraphReference !is FirControlFlowGraphReferenceImpl) {
        return emptyList()
    }
    val cfg = (this.controlFlowGraphReference as FirControlFlowGraphReferenceImpl).controlFlowGraph
    return getVariableDeclarations(this, cfg)
}

/**
 * Returns variables declarations inside function
 */
fun getVariableDeclarations(firSimpleFunction: FirSimpleFunction, cfg: ControlFlowGraph): List<FirVariable<*>> {
    val result = mutableListOf<FirVariable<*>>()
    // TODO better way to find all vars and vals
    result.addAll(cfg.nodes.map { it.fir }.filterIsInstance<FirVariable<*>>().filter { !it.name.isSpecial })
    return result
}

fun CFGNode<*>.isEnterOrExitNode(): Boolean {
    return (this is EnterNodeMarker) || (this is ExitNodeMarker)
}

fun CFGNode<*>.isExitNode(): Boolean {
    return this is ExitNodeMarker
}

// gets the corresponding psi element if any
fun FirStatement.getPsiElement(): PsiElement? {
    return this.source.psi
}

fun ControlFlowGraph.getTraversedNodes(): MutableList<CFGNode<*>> {
    val nodes = mutableListOf<CFGNode<*>>()
    val visitor = object : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {
            nodes.add(node)
        }
    }
    this.traverse(TraverseDirection.Forward, visitor)
    return nodes
}

fun ControlFlowGraph.getDfgEdges() {
    val node = this.enterNode
    val visitedNodes = mutableSetOf<CFGNode<*>>()
    val stack = stackOf(node)
    println("here")
    while (stack.isNotEmpty) {
        val curr = stack.pop()
        visitedNodes.add(curr)
        for (n in curr.followingNodes) {
            if (n !in visitedNodes) {
                stack.push(n)
            }
            if (curr.outgoingEdges[n] == EdgeKind.Dfg || curr.outgoingEdges[n] == EdgeKind.Cfg) {
                println("$curr $n\n")
            }
        }
    }
}