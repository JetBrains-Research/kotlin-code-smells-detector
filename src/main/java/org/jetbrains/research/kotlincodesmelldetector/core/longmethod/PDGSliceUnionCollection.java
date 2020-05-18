package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*;
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.BasicBlock;
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.PDGSliceUnion;

import java.util.*;

public class PDGSliceUnionCollection {
    private final Map<BasicBlock, PDGSliceUnion> sliceUnionMap;
    private final BasicBlockCFG basicBlockCFG;
    private final FirSimpleFunction firSimpleFunction;
    // TODO do not need this, just for testing basic blocks
    private final List<BasicBlock> basicBlocks = new ArrayList<>();

    public PDGSliceUnionCollection(FirSimpleFunction firSimpleFunction, ControlFlowGraph cfg, FirVariable<?> variable) {
        this.sliceUnionMap = new LinkedHashMap<>();
        this.basicBlockCFG = new BasicBlockCFG(cfg);
        this.firSimpleFunction = firSimpleFunction;
        Set<CFGNode<?>> nodeCriteria = getAssignmentNodesOfVariableCriterion(cfg, variable);
        Map<CFGNode<?>, Set<BasicBlock>> boundaryBlockMap = new LinkedHashMap<>();
        for (CFGNode<?> nodeCriterion : nodeCriteria) {
            Set<BasicBlock> boundaryBlocks = boundaryBlocks(cfg, nodeCriterion);
            boundaryBlockMap.put(nodeCriterion, boundaryBlocks);
        }
        List<Set<BasicBlock>> list = new ArrayList<>(boundaryBlockMap.values());
        if (!list.isEmpty()) {
            Set<BasicBlock> basicBlockIntersection = new LinkedHashSet<>(list.get(0));
            for (int i = 1; i < list.size(); i++) {
                basicBlockIntersection.retainAll(list.get(i));
            }
            for (BasicBlock basicBlock : basicBlockIntersection) {
                PDGSliceUnion sliceUnion = new PDGSliceUnion(firSimpleFunction, cfg, basicBlock, nodeCriteria, variable);
                // TODO omit this check for now
//                if (sliceUnion.satisfiesRules())
                    //sliceUnionMap.put(basicBlock, sliceUnion);
                sliceUnionMap.put(basicBlock, sliceUnion);
                basicBlocks.add(basicBlock);
            }
        }
    }


    private Set<BasicBlock> boundaryBlocks(ControlFlowGraph cfg, CFGNode<?> node) {
        Set<BasicBlock> boundaryBlocks = new LinkedHashSet<>();
        BasicBlock srcBlock = basicBlockCFG.nodeToBlock.get(node);
        for (BasicBlock block : basicBlockCFG.getBasicBlocks()) {
            Set<BasicBlock> forwardReachableBlocks = forwardReachableBlocks(block);
            Set<BasicBlock> dominatedBlocks = dominatedBlocks(block);
            Set<BasicBlock> intersection = new LinkedHashSet<>(forwardReachableBlocks);
            intersection.retainAll(dominatedBlocks);
            if (intersection.contains(srcBlock))
                boundaryBlocks.add(block);
        }
        return boundaryBlocks;
    }

    private Set<BasicBlock> dominatedBlocks(BasicBlock block) {
        CFGNode<?> pdgNode = directlyDominates(block);
        // TODO do not use cache for now
//        if (dominatedBlockMap.containsKey(pdgNode)) {
//            return dominatedBlockMap.get(pdgNode);
//        } else {
            Set<BasicBlock> dominatedBlocks = null;
            if (pdgNode != null) {
                dominatedBlocks = dominatedBlocks(pdgNode);
            }
           // dominatedBlockMap.put(pdgNode, dominatedBlocks);
            return dominatedBlocks;
      //  }
    }

    //returns the node (branch or method entry) that directly dominates the leader of the block
    private CFGNode<?> directlyDominates(BasicBlock block) {
        CFGNode<?> leaderCFGNode = block.getLeader();
        if (leaderCFGNode.getPreviousNodes().isEmpty()) {
            return null;
        }
        return leaderCFGNode.getPreviousNodes().get(0);
    }

    private Set<BasicBlock> dominatedBlocks(CFGNode<?> branchNode) {
        Set<BasicBlock> dominatedBlocks = new LinkedHashSet<>();
        for (CFGNode<?> node : branchNode.getFollowingNodes()) {
            // TODO is following check correct?
                BasicBlock dstBlock = basicBlockCFG.nodeToBlock.get(node);
                dominatedBlocks.add(dstBlock);
                CFGNode<?> dstBlockLastNode = dstBlock.getLastNode();
                // TODO is following check correct?
                if (dstBlockLastNode instanceof EnterNodeMarker && !dstBlockLastNode.equals(branchNode))
                    dominatedBlocks.addAll(dominatedBlocks(dstBlockLastNode));

        }
        return dominatedBlocks;
    }

    private Set<CFGNode<?>> getAssignmentNodesOfVariableCriterion(ControlFlowGraph cfg, FirVariable<?> variable) {
        Set<CFGNode<?>> nodeCriteria = new LinkedHashSet<>();
        for (CFGNode<?> node : cfg.getNodes()) {
            if (isNodeDefinesButNotDeclaresVariable(node, variable)) {
                nodeCriteria.add(node);
            }
        }
        return nodeCriteria;
    }

    // TODO make it work the same now it's simplified a lot
    // in the original plugin рассматриваются всякие поля классов, на которые могут влиять
    // и всякое такое
    private boolean isNodeDefinesButNotDeclaresVariable(CFGNode<?> node, FirVariable<?> variable) {
        return node instanceof VariableAssignmentNode;
//        return (definesLocalVariable(node, variable) &&
//                !declaresLocalVariable(node,variable));
    }


    public Collection<PDGSliceUnion> getSliceUnions() {
        return sliceUnionMap.values();
    }

    public Collection<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    private Set<BasicBlock> forwardReachableBlocks(BasicBlock basicBlock) {
        return basicBlockCFG.forwardReachableBlocks(basicBlock);
    }

    public FirSimpleFunction getFirSimpleFunction() {
        return firSimpleFunction;
    }
}
