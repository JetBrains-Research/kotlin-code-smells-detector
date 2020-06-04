package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterNodeMarker;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode;

import java.util.*;

public class PDGSliceUnionCollection {
    private final Map<BasicBlock, PDGSliceUnion> sliceUnionMap;
    private final BasicBlockCFG basicBlockCFG;
    private final FirSimpleFunction firSimpleFunction;
    private final PDG pdg;


    public PDGSliceUnionCollection(FirSimpleFunction firSimpleFunction, ControlFlowGraph cfg, FirVariable<?> variable) {
        this.sliceUnionMap = new LinkedHashMap<>();
        this.basicBlockCFG = new BasicBlockCFG(cfg);
        this.firSimpleFunction = firSimpleFunction;
        this.pdg = new PDG(cfg, firSimpleFunction);
        Set<PDGNode> nodeCriteria = getAssignmentNodesOfVariableCriterion(pdg, variable);
        Map<CFGNode<?>, Set<BasicBlock>> boundaryBlockMap = new LinkedHashMap<>();
        for (PDGNode nodeCriterion : nodeCriteria) {
            Set<BasicBlock> boundaryBlocks = boundaryBlocks(cfg, nodeCriterion.getCfgNode());
            boundaryBlockMap.put(nodeCriterion.getCfgNode(), boundaryBlocks);
        }
        List<Set<BasicBlock>> list = new ArrayList<>(boundaryBlockMap.values());
        if (!list.isEmpty()) {
            Set<BasicBlock> basicBlockIntersection = new LinkedHashSet<>(list.get(0));
            for (int i = 1; i < list.size(); i++) {
                basicBlockIntersection.retainAll(list.get(i));
            }
            for (BasicBlock basicBlock : basicBlockIntersection) {
                PDGSliceUnion sliceUnion = new PDGSliceUnion(firSimpleFunction, cfg, basicBlock, nodeCriteria, variable, basicBlockCFG);
                // TODO check sliceUnion.satisfiesRules()
                sliceUnionMap.put(basicBlock, sliceUnion);
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
            if (intersection.contains(srcBlock)) {
                boundaryBlocks.add(block);
            }
        }
        return boundaryBlocks;
    }

    // TODO use map for cache
    private Set<BasicBlock> dominatedBlocks(BasicBlock block) {
        CFGNode<?> pdgNode = directlyDominates(block);
        Set<BasicBlock> dominatedBlocks = null;
        if (pdgNode != null) {
            dominatedBlocks = dominatedBlocks(pdgNode);
        }
        return dominatedBlocks;
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
            BasicBlock dstBlock = basicBlockCFG.nodeToBlock.get(node);
            dominatedBlocks.add(dstBlock);
            CFGNode<?> dstBlockLastNode = dstBlock.getLastNode();
            // TODO check condition
            if (dstBlockLastNode instanceof EnterNodeMarker && !dstBlockLastNode.equals(branchNode)) {
                dominatedBlocks.addAll(dominatedBlocks(dstBlockLastNode));
            }

        }
        return dominatedBlocks;
    }

    private Set<PDGNode> getAssignmentNodesOfVariableCriterion(PDG pdg, FirVariable<?> variable) {
        Set<PDGNode> nodeCriteria = new LinkedHashSet<>();
        for (PDGNode node : pdg.getNodes()) {
            if (isNodeDefinesButNotDeclaresVariable(node, variable)) {
                nodeCriteria.add(node);
            }
        }
        return nodeCriteria;
    }

    // TODO handle class fields
    private boolean isNodeDefinesButNotDeclaresVariable(PDGNode node, FirVariable<?> variable) {
        if (node.getCfgNode() instanceof VariableAssignmentNode) {
            FirVariableAssignment variableAssignment = ((VariableAssignmentNode) node.getCfgNode()).getFir();
            if (variableAssignment.getLValue() instanceof FirResolvedNamedReference) {
                FirResolvedNamedReference lValue = (FirResolvedNamedReference) variableAssignment.getLValue();
                return lValue.getName().equals(variable.getName());
            }
        }
        return false;
    }


    public Collection<PDGSliceUnion> getSliceUnions() {
        return sliceUnionMap.values();
    }

    private Set<BasicBlock> forwardReachableBlocks(BasicBlock basicBlock) {
        return basicBlockCFG.forwardReachableBlocks(basicBlock);
    }

    public FirSimpleFunction getFirSimpleFunction() {
        return firSimpleFunction;
    }
}
