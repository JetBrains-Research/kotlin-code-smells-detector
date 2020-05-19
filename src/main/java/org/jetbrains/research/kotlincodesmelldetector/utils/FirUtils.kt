package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
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
    val transformer = FirTotalResolveTransformer()
    val firFile = ktFile?.getOrBuildFir(FirModuleResolveStateImpl(FirProjectSessionProvider(project)))!!
    transformer.processFiles(listOf(firFile))
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
fun getVariableDeclarationsInFunction(firSimpleFunction: FirSimpleFunction): List<FirVariable<*>> {
    if (firSimpleFunction.controlFlowGraphReference !is FirControlFlowGraphReferenceImpl) {
        return emptyList()
    }
    val cfg = (firSimpleFunction.controlFlowGraphReference as FirControlFlowGraphReferenceImpl).controlFlowGraph
    return getVariableDeclarationsInFunction(firSimpleFunction, cfg)
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
