package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*;

import java.util.*;

class BasicBlockCFG {
    private final List<BasicBlock> basicBlocks;
    private final Map<BasicBlock, Set<BasicBlock>> forwardReachableBlocks;
    public final Map<CFGNode<?>, BasicBlock> nodeToBlock = new LinkedHashMap<>();

    BasicBlockCFG(ControlFlowGraph cfg) {
        this.basicBlocks = new ArrayList<>();
        this.forwardReachableBlocks = new LinkedHashMap<>();
        Set<CFGNode<?>> allNodes = new HashSet<>(cfg.getNodes());
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
            if (node instanceof EnterNodeMarker) {
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