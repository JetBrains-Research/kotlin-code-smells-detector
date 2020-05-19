package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasicBlock {
    private static int blockNum = 0;
    private final int id;
    private final CFGNode<?> leader;
    private final List<CFGNode<?>> nodes;
    private BasicBlock previousBasicBlock;
    private BasicBlock nextBasicBlock;

    public BasicBlock(CFGNode<?> node, Map<CFGNode<?>, BasicBlock> nodeToBlock) {
        blockNum++;
        this.id = blockNum;
        this.leader = node;
        nodeToBlock.put(node,this);
        this.nodes = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public CFGNode<?> getLeader() {
        return leader;
    }

    public List<CFGNode<?>> getNodes() {
        return nodes;
    }

    public List<CFGNode<?>> getAllNodes() {
        List<CFGNode<?>> allNodes = new ArrayList<>();
        allNodes.add(leader);
        allNodes.addAll(nodes);
        return allNodes;
    }

    public List<CFGNode<?>> getAllNodesIncludingTry() {
        List<CFGNode<?>> allNodes = new ArrayList<>();
        allNodes.add(leader);
        allNodes.addAll(nodes);
        //allNodes.addAll(tryNodes);
        return allNodes;
    }

    public CFGNode<?> getLastNode() {
        if (!nodes.isEmpty())
            return nodes.get(nodes.size() - 1);
        else
            return leader;
    }

    public void add(CFGNode<?> node, Map<CFGNode<?>, BasicBlock> nodeToBlock) {
        nodes.add(node);
        nodeToBlock.put(node,this);
    }

    // TODO add tryNodes to BasicBlock
//    public void addTryNode(CFGTryNode tryNode) {
//        tryNodes.add(tryNode);
//        tryNode.setBasicBlock(this);
//    }

    public BasicBlock getPreviousBasicBlock() {
        return previousBasicBlock;
    }

    public void setPreviousBasicBlock(BasicBlock previousBasicBlock) {
        this.previousBasicBlock = previousBasicBlock;
    }

    public BasicBlock getNextBasicBlock() {
        return nextBasicBlock;
    }

    public void setNextBasicBlock(BasicBlock nextBasicBlock) {
        this.nextBasicBlock = nextBasicBlock;
    }

    public static void resetBlockNum() {
        blockNum = 0;
    }

    public String toString() {
        return leader.toString() + nodes.toString();
    }
}