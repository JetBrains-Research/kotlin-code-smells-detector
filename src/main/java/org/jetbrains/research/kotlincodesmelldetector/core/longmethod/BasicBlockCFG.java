package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*;
import org.jetbrains.research.kotlincodesmelldetector.utils.FirUtilsKt;

import java.util.*;

class BasicBlockCFG {
    public final Map<CFGNode<?>, BasicBlock> nodeToBlock = new LinkedHashMap<>();
    private final List<BasicBlock> basicBlocks;
    private final Map<BasicBlock, Set<BasicBlock>> forwardReachableBlocks;

    BasicBlockCFG(ControlFlowGraph cfg) {
        this.basicBlocks = new ArrayList<>();
        this.forwardReachableBlocks = new LinkedHashMap<>();
        //List<CFGNode<?>> allNodes = cfg.getNodes();
        List<CFGNode<?>> allNodes = FirUtilsKt.sortedNodes(cfg);
        //Map<CFGBlockNode, List<CFGNode>> directlyNestedNodesInBlocks = cfg.getDirectlyNestedNodesInBlocks();
        // TODO no special handling for try for now
        //        for (CFGBlockNode blockNode : directlyNestedNodesInBlocks.keySet()) {
        //            if (blockNode instanceof CFGTryNode) {
        //                CFGTryNode tryNode = (CFGTryNode) blockNode;
        //                if (!tryNode.hasResources())
        //                    allNodes.add(tryNode);
        //            }
        //        }
        for (CFGNode<?> node : allNodes) {
            //            if (node instanceof CFGTryNode && !((CFGTryNode) cfgNode).hasResources()) {
            //                CFGTryNode tryNode = (CFGTryNode) cfgNode;
            //                if (!basicBlocks.isEmpty()) {
            //                    BasicBlock basicBlock = basicBlocks.get(basicBlocks.size() - 1);
            //                    basicBlock.addTryNode(tryNode);
            //                }
            //            } else
            if (node instanceof EnterNodeMarker || node instanceof ExitNodeMarker) { // Do not add these special nodes to block
                continue;
            }
            if (isFirst(node)) {
                BasicBlock basicBlock = new BasicBlock(node, nodeToBlock);
                if (!basicBlocks.isEmpty()) {
                    BasicBlock previousBlock = basicBlocks.get(basicBlocks.size() - 1);
                    previousBlock.setNextBasicBlock(basicBlock);
                    basicBlock.setPreviousBasicBlock(previousBlock);
                }
                basicBlocks.add(basicBlock);
            } else {
                BasicBlock basicBlock = basicBlocks.get(basicBlocks.size() - 1);
                basicBlock.add(node, nodeToBlock);
            }
        }
        //special handling for the try statement that is first node
        //        for (CFGBlockNode blockNode : directlyNestedNodesInBlocks.keySet()) {
        //            if (blockNode instanceof CFGTryNode) {
        //                CFGTryNode tryNode = (CFGTryNode) blockNode;
        //                if (tryNode.id == 1 && !basicBlocks.isEmpty() && !tryNode.hasResources()) {
        //                    BasicBlock basicBlock = basicBlocks.get(0);
        //                    basicBlock.addTryNode(tryNode);
        //                }
        //            }
        //        }
        BasicBlock.resetBlockNum();
    }

    // Checks if the node directly follows an EnterNode
    private boolean isFirst(CFGNode<?> node) {
        for (CFGNode<?> cfgNode : node.getPreviousNodes()) {
            if (cfgNode instanceof EnterNodeMarker) {
                return true;
            }
        }
        return false;
    }

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public Set<BasicBlock> forwardReachableBlocks(BasicBlock basicBlock) {
        if (forwardReachableBlocks.containsKey(basicBlock)) {
            return forwardReachableBlocks.get(basicBlock);
        }
        Set<BasicBlock> reachableBlocks = new LinkedHashSet<>();
        reachableBlocks.add(basicBlock);
        CFGNode<?> lastNode = basicBlock.getLastNode();
        Map<CFGNode<?>, EdgeKind> outgoingEdges = lastNode.getOutgoingEdges();
        Set<Map.Entry<CFGNode<?>, EdgeKind>> entries = outgoingEdges.entrySet();
        for (Map.Entry<CFGNode<?>, EdgeKind> entry : entries) {
            // TODO check if condition is correct, we not what loop back here
            if (!(entry.getKey() instanceof LoopBlockEnterNode)) {
                CFGNode<?> dstNode = entry.getKey();
                BasicBlock dstBasicBlock = nodeToBlock.get(dstNode);
                reachableBlocks.add(dstBasicBlock);
                reachableBlocks.addAll(forwardReachableBlocks(dstBasicBlock));
            }
        }
        forwardReachableBlocks.put(basicBlock, reachableBlocks);
        return reachableBlocks;
    }
}