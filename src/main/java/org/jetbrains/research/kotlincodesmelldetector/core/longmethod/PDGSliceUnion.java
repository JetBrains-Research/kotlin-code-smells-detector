package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph;

import java.util.*;
import java.util.stream.Collectors;

public class PDGSliceUnion {
    private final ControlFlowGraph cfg;
    private final List<CFGNode<?>> sliceNodes;
    private final FirSimpleFunction function;
    private final FirVariable<?> localVariableCriterion;
    private final BasicBlock boundaryBlock;
    private final BasicBlockCFG basicBlockCFG;
    private Set<CFGNode<?>> specialNodes;


    public PDGSliceUnion(FirSimpleFunction firSimpleFunction, ControlFlowGraph cfg, BasicBlock basicBlock, Set<PDGNode> nodeCriteria, FirVariable<?> localVariableCriterion, BasicBlockCFG basicBlockCFG) {
        this.cfg = cfg;
        this.function = firSimpleFunction;
        this.basicBlockCFG = basicBlockCFG;
        this.sliceNodes = new ArrayList<>();
        this.localVariableCriterion = localVariableCriterion;
        this.boundaryBlock = basicBlock;
        this.specialNodes = blockBasedRegion(boundaryBlock);
        for (PDGNode nodeCriterion : nodeCriteria) {
            sliceNodes.addAll(computeSlice(nodeCriterion).stream().map(PDGNode::getCfgNode).collect(Collectors.toList()));
        }

    }

    Set<CFGNode<?>> blockBasedRegion(BasicBlock block) {
        Set<CFGNode<?>> regionNodes = new LinkedHashSet<>();
        Set<BasicBlock> reachableBlocks = basicBlockCFG.forwardReachableBlocks(block);
        for (BasicBlock reachableBlock : reachableBlocks) {
            List<CFGNode<?>> blockNodes = reachableBlock.getAllNodesIncludingTry();
            regionNodes.addAll(blockNodes);
        }
        return regionNodes;
    }

    private Collection<? extends PDGNode> computeSlice(PDGNode nodeCriterion) {
        return new LinkedHashSet<>(traverseBackward(nodeCriterion, new LinkedHashSet<>()));
    }


    private Set<PDGNode> traverseBackward(PDGNode node, Set<PDGNode> visitedNodes) {
        Set<PDGNode> sliceNodes = new LinkedHashSet<>();
        sliceNodes.add(node);
        visitedNodes.add(node);
        for (PDGNode.GraphEdge edge : node.incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (specialNodes.contains(dependence.src.getCfgNode()) && specialNodes.contains(dependence.dst.getCfgNode())) {
                PDGNode srcPDGNode = dependence.src;
                if (!visitedNodes.contains(srcPDGNode)) {
                    sliceNodes.addAll(traverseBackward(srcPDGNode, visitedNodes));
                }
            }
        }
        return sliceNodes;
    }

    public List<CFGNode<?>> getSliceNodes() {
        return sliceNodes;
    }

    public FirSimpleFunction getFunction() {
        return function;
    }

    public FirVariable<?> getLocalVariableCriterion() {
        return localVariableCriterion;
    }
}
