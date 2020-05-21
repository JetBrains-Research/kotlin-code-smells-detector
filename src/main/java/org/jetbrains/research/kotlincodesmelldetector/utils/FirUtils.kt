package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.analysis.cfa.traverse

import org.jetbrains.kotlin.fir.analysis.cfa.TraverseDirection
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.idea.fir.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.getOrBuildFir
import org.jetbrains.kotlin.utils.DFS

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
    // val transformer = FirTotalResolveTransformer()
    val firFile = ktFile?.getOrBuildFir(FirModuleResolveStateImpl(FirProjectSessionProvider(project)), FirResolvePhase.BODY_RESOLVE)!!
    // transformer.processFiles(listOf(firFile))
    return firFile
}

/**
 * Returns variables inside as well as function parameters
 */
fun getVariableDeclarationsInFunction(firSimpleFunction: FirSimpleFunction, cfg: ControlFlowGraph): List<FirVariable<*>> {
    val result = mutableListOf<FirVariable<*>>()
    result.addAll(firSimpleFunction.valueParameters)
    // TODO better way to find all vars and vals
    result.addAll(cfg.nodes.map { it.fir }.filterIsInstance<FirVariable<*>>().filter { !it.name.isSpecial })
    return result
}

/**
 * Returns variables inside as well as function parameters
 */
fun FirSimpleFunction.getVariableDeclarationsInFunction() : List<FirVariable<*>> {
    if (this.controlFlowGraphReference !is FirControlFlowGraphReferenceImpl) {
        return emptyList()
    }
    val cfg = (this.controlFlowGraphReference as FirControlFlowGraphReferenceImpl).controlFlowGraph
    return getVariableDeclarationsInFunction(this, cfg)
}

// the method is copy-paste of a private method in ControlFlowGraphRenderer.kt
// TODO filter out dfg edges
fun ControlFlowGraph.sortedNodes(): List<CFGNode<*>> {
    val nodesToSort = nodes.filterTo(mutableListOf()) { it != enterNode }
    val graphs = mutableSetOf(this)
    forEachSubGraph {
        nodesToSort += it.nodes
        graphs += it
    }

    val topologicalOrder = DFS.topologicalOrder(nodesToSort) {
        val result = if (it !is WhenBranchConditionExitNode || it.followingNodes.size < 2) {
            it.followingNodes
        } else {
            it.followingNodes.sortedBy { node -> if (node is BlockEnterNode) 1 else 0 }
        }.filter { node -> node.owner in graphs }
        result
    }
    return listOf(enterNode) + topologicalOrder
}

private fun ControlFlowGraph.forEachSubGraph(block: (ControlFlowGraph) -> Unit) {
    for (subGraph in subGraphs) {
        block(subGraph)
        subGraph.forEachSubGraph(block)
    }
}

fun CFGNode<*>.isEnterOrExitNode(): Boolean {
    return (this is EnterNodeMarker) || (this is ExitNodeMarker)
}

// gets the corresponding psi element if any
fun FirStatement.getPsiElement(): PsiElement? {
    return this.source.psi
}

fun testTraverse(cfg: ControlFlowGraph) {
    val set = HashSet<CFGNode<*>>()
    val visitorVoid = object : ControlFlowGraphVisitorVoid() {
        override fun visitNode(p0: CFGNode<*>) {
            set.add(p0)
            //println(p0.fir.source.toString())
        }
    }
    cfg.traverse(TraverseDirection.Forward, visitorVoid)
    if (cfg.nodes.toSet() != set) {
        println("Not equal")
    } else {
        println("Equal")
    }
}