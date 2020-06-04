package org.jetbrains.research.kotlincodesmelldetector.core.longmethod
//
//import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
//import org.jetbrains.kotlin.fir.declarations.FirVariable
//import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
//import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
//import java.util.*
//
//class PDGSlice internal constructor(cfg: ControlFlowGraph, boundaryBlock: BasicBlock) {
//    val cfg : ControlFlowGraph
//    val method: FirSimpleFunction
//    private val boundaryBlock: BasicBlock
//    private val localVariableCriterion: FirVariable<*>
//    private val sliceNodes: Set<CFGNode<*>>
//    private val edges : Set<Pair<CFGNode<*>, CFGNode<*>>>
//
//    fun nodeBelongsToBlockBasedRegion(node: GraphNode?): Boolean {
//        return nodes.contains(node)
//    }
//
//    fun edgeBelongsToBlockBasedRegion(edge: GraphEdge?): Boolean {
//        return edges.contains(edge)
//    }
//
//    fun computeSlice(nodeCriterion: PDGNode, localVariableCriterion: AbstractVariable): Set<PDGNode> {
//        val sliceNodes: MutableSet<PDGNode> = LinkedHashSet<PDGNode>()
//        if (nodeCriterion.definesLocalVariable(localVariableCriterion)) {
//            sliceNodes.addAll(traverseBackward(nodeCriterion, LinkedHashSet<PDGNode>()))
//        } else if (nodeCriterion.usesLocalVariable(localVariableCriterion)) {
//            val defNodes: Set<PDGNode> = getDefNodes(nodeCriterion, localVariableCriterion)
//            for (defNode in defNodes) {
//                sliceNodes.addAll(traverseBackward(defNode, LinkedHashSet<PDGNode>()))
//            }
//            sliceNodes.addAll(traverseBackward(nodeCriterion, LinkedHashSet<PDGNode>()))
//        }
//        return sliceNodes
//    }
//
//    fun computeSlice(nodeCriterion: PDGNode): Set<PDGNode> {
//        return LinkedHashSet<Any?>(traverseBackward(nodeCriterion, LinkedHashSet<PDGNode>()))
//    }
//
//    private fun getDefNodes(node: PDGNode, localVariable: AbstractVariable): Set<PDGNode> {
//        val defNodes: MutableSet<PDGNode> = LinkedHashSet<PDGNode>()
//        for (edge in node.incomingEdges) {
//            val dependence: PDGDependence = edge as PDGDependence
//            if (edges.contains(dependence) && dependence is PDGDataDependence) {
//                val dataDependence: PDGDataDependence = dependence as PDGDataDependence
//                if (dataDependence.getData().equals(localVariable)) {
//                    val srcPDGNode: PDGNode = dependence.src as PDGNode
//                    defNodes.add(srcPDGNode)
//                }
//            }
//        }
//        return defNodes
//    }
//
//    private fun traverseBackward(node: CFGNode<*>, visitedNodes: MutableSet<CFGNode<*>>): Set<CFGNode<*>> {
//        val sliceNodes = LinkedHashSet<CFGNode<*>>()
//        sliceNodes.add(node)
//        visitedNodes.add(node)
//        for (previousNode in node.previousNodes) {
//            val edgeType = node.incomingEdges[previousNode]
//            if (edges.contains(Pair(previousNode, node)) && dependence !is PDGAntiDependence
//                    && dependence !is PDGOutputDependence) {
//                val srcPDGNode: PDGNode = dependence.src as PDGNode
//                if (!visitedNodes.contains(srcPDGNode)) {
//                    sliceNodes.addAll(traverseBackward(srcPDGNode, visitedNodes))
//                }
//            }
//        }
//        return sliceNodes
//    }
//
//    override fun toString(): String {
//        return """
//            <$localVariableCriterion, ${nodeCriterion.getId()}> [B${boundaryBlock.getId()}]
//            $sliceNodes
//            passed parameters: $passedParameters
//            indispensable nodes: $indispensableNodes
//            """.trimIndent()
//    }
//
//    init {
//        this.pdg = pdg
//        method = pdg.getMethod()
//        psiFile = pdg.getPsiFile()
//        methodSize = pdg.getTotalNumberOfStatements()
//        returnedVariablesInOriginalMethod = pdg.getReturnedVariables()
//        this.boundaryBlock = boundaryBlock
//        val regionNodes: Set<PDGNode> = pdg.blockBasedRegion(boundaryBlock)
//        nodes.addAll(regionNodes)
//        for (edge in pdg.edges) {
//            val dependence: PDGDependence = edge as PDGDependence
//            if (nodes.contains(dependence.src) && nodes.contains(dependence.dst)) {
//                if (dependence is PDGDataDependence) {
//                    val dataDependence: PDGDataDependence = dependence as PDGDataDependence
//                    if (dataDependence.isLoopCarried()) {
//                        val loopNode: PDGNode = dataDependence.getLoop().getPDGNode()
//                        if (nodes.contains(loopNode)) edges.add(dataDependence)
//                    } else edges.add(dataDependence)
//                } else if (dependence is PDGAntiDependence) {
//                    val antiDependence: PDGAntiDependence = dependence as PDGAntiDependence
//                    if (antiDependence.isLoopCarried()) {
//                        val loopNode: PDGNode = antiDependence.getLoop().getPDGNode()
//                        if (nodes.contains(loopNode)) edges.add(antiDependence)
//                    } else edges.add(antiDependence)
//                } else if (dependence is PDGOutputDependence) {
//                    val outputDependence: PDGOutputDependence = dependence as PDGOutputDependence
//                    if (outputDependence.isLoopCarried()) {
//                        val loopNode: PDGNode = outputDependence.getLoop().getPDGNode()
//                        if (nodes.contains(loopNode)) edges.add(outputDependence)
//                    } else edges.add(outputDependence)
//                } else edges.add(dependence)
//            }
//        }
//    }
//}