package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph;
import org.jetbrains.research.kotlincodesmelldetector.utils.FirUtilsKt;

import java.util.*;

public class PDG {
    private final ControlFlowGraph cfg;
    private final FirSimpleFunction function;
    private final PDGNode entryNode;
    private final Map<FirWhenBranch, Set<CFGNode<?>>> nestingMap;
    private final List<FirVariable<?>> variableDeclarationsInMethod;
    private final Map<CFGNode<?>, Set<BasicBlock>> dominatedBlockMap;
    private final List<PDGNode> nodes;
    private final List<PDGNode.GraphEdge> edges;
    private final Map<CFGNode<?>, PDGNode> cfgToPdgNode = new HashMap<>();

    public PDG(ControlFlowGraph cfg, FirSimpleFunction function) {
        this.cfg = cfg;
        this.nestingMap = new LinkedHashMap<>();
        this.function = function;
        this.variableDeclarationsInMethod = FirUtilsKt.getVariableDeclarationsAndParameters(function);
        this.entryNode = new PDGNode(cfg.enterNode, variableDeclarationsInMethod, cfgToPdgNode);
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
       // for (CFGNode<?> cfgNode : FirUtilsKt.getTraversedNodes(cfg)) {
            // TODO
//            if (cfgNode.getFir() instanceof FirWhenBranch) {
//                FirWhenBranch branchNode = (FirWhenBranch) cfgNode.getFir();
//                nestingMap.put(branchNode, branchNode.getImmediatelyNestedNodesFromAST());
//            }
       // }
        createControlDependenciesFromEntryNode();
        if (!nodes.isEmpty()) {
            createDataDependencies();
        }
        this.dominatedBlockMap = new LinkedHashMap<>();
        PDGNode.resetNodeNum();
        // TODO
        //handleJumpNodes();
    }

    public PDGNode getEntryNode() {
        return entryNode;
    }

    public FirSimpleFunction getMethod() {
        return function;
    }

    public List<PDGNode> getNodes() {
        return nodes;
    }

    int getTotalNumberOfStatements() {
        return nodes.size();
    }

    private void createControlDependenciesFromEntryNode() {
        for (CFGNode<?> cfgNode : FirUtilsKt.getTraversedNodes(cfg)) {
            if (!isNested(cfgNode)) {
                processCFGNode(entryNode, cfgNode, true);
            }
        }
    }

    private void processCFGNode(PDGNode previousNode, CFGNode<?> cfgNode, boolean controlType) {
        if (cfgNode.getFir() instanceof FirWhenBranch) {
            PDGNode predicateNode = new PDGNode(cfgNode, variableDeclarationsInMethod, cfgToPdgNode);
            nodes.add(predicateNode);
            PDGControlDependence controlDependence = new PDGControlDependence(previousNode, predicateNode, controlType);
            edges.add(controlDependence);
            // TODO
           // processControlPredicate(predicateNode);
        } else {
            PDGNode pdgNode;
            if (FirUtilsKt.isExitNode(cfgNode)) {
                pdgNode = new PDGNode(cfgNode, variableDeclarationsInMethod, cfgToPdgNode);
            } else {
                pdgNode = new PDGNode(cfgNode, variableDeclarationsInMethod, cfgToPdgNode);
            }
            nodes.add(pdgNode);
            PDGControlDependence controlDependence = new PDGControlDependence(previousNode, pdgNode, controlType);
            edges.add(controlDependence);
        }
    }

//    private void processControlPredicate(PDGNode predicateNode) {
//        FirWhenBranch branchNode = (FirWhenBranch) predicateNode.getCfgNode().getFir();
//        // TODO if branch
//        //if (branchNode instanceof CFGBranchIfNode) {
//        if (false) {
//            //            CFGBranchIfNode conditionalNode = (CFGBranchIfNode) branchNode;
//            //            Set<CFGNode<?>> nestedNodesInTrueControlFlow = conditionalNode.getImmediatelyNestedNodesInTrueControlFlow();
//            //            for (CFGNode<?> nestedNode : nestedNodesInTrueControlFlow) {
//            //                processCFGNode(predicateNode, nestedNode, true);
//            //            }
//            //            Set<CFGNode<?>> nestedNodesInFalseControlFlow = conditionalNode.getImmediatelyNestedNodesInFalseControlFlow();
//            //            for (CFGNode<?> nestedNode : nestedNodesInFalseControlFlow) {
//            //                processCFGNode(predicateNode, nestedNode, false);
//            //            }
//        } else {
//            Set<CFGNode<?>> nestedNodes = nestingMap.get(branchNode);
//            for (CFGNode<?> nestedNode : nestedNodes) {
//                processCFGNode(predicateNode, nestedNode, true);
//            }
//        }
//    }

    private boolean isNested(CFGNode<?> node) {
        for (FirWhenBranch key : nestingMap.keySet()) {
            Set<CFGNode<?>> nestedNodes = nestingMap.get(key);
            if (nestedNodes.contains(node)) {
                return true;
            }
        }
        return false;
    }

    private void createDataDependencies() {
        PDGNode firstPDGNode = (PDGNode) nodes.toArray()[0];
        createDataDependenciesFromEntryNode(firstPDGNode);
        for (PDGNode pdgNode : nodes) {
            for (FirVariable<?> variableInstruction : pdgNode.definedVariables()) {
                dataDependenceSearch(pdgNode, variableInstruction, pdgNode, new LinkedHashSet<>(), null);
            }
        }
    }

    private void createDataDependenciesFromEntryNode(PDGNode pdgNode) {
        for (FirVariable<?> variableInstruction : FirUtilsKt.getVariableDeclarations(function)) {
            if (pdgNode.usesLocalVariable(variableInstruction)) {
                PDGDataDependence dataDependence = new PDGDataDependence(entryNode, pdgNode, variableInstruction, null);
                edges.add(dataDependence);
            }
            if (!pdgNode.definesLocalVariable(variableInstruction)) {
                dataDependenceSearch(entryNode, variableInstruction, pdgNode, new LinkedHashSet<>(), null);
            } else if (entryNode.declaresLocalVariable(variableInstruction)) {
                //create def-order data dependence edge
                PDGDataDependence dataDependence = new PDGDataDependence(entryNode, pdgNode, variableInstruction, null);
                edges.add(dataDependence);
            }
        }
    }

    private void dataDependenceSearch(PDGNode initialNode, FirVariable<?> variableInstruction,
                                      PDGNode currentNode, Set<PDGNode> visitedNodes, FirWhenBranch loop) {
        if (visitedNodes.contains(currentNode)) {
            return;
        } else {
            visitedNodes.add(currentNode);
        }
        CFGNode<?> currentCFGNode = currentNode.getCfgNode();
        for (CFGNode<?> dstCFGNode : currentCFGNode.getFollowingNodes()) {
            // TODO
            //            if (flow.isLoopbackFlow()) {
            //                if (dstCFGNode instanceof CFGBranchLoopNode)
            //                    loop = (CFGBranchLoopNode) dstCFGNode;
            //                if (srcCFGNode instanceof CFGBranchDoLoopNode)
            //                    loop = (CFGBranchDoLoopNode) srcCFGNode;
            //            }
            PDGNode dstPDGNode = cfgToPdgNode.get(dstCFGNode);
            if (dstPDGNode != null) {
                if (dstPDGNode.usesLocalVariable(variableInstruction)) {
                    PDGDataDependence dataDependence = new PDGDataDependence(initialNode, dstPDGNode, variableInstruction, loop);
                    edges.add(dataDependence);
                }
                if (!dstPDGNode.definesLocalVariable(variableInstruction)) {
                    dataDependenceSearch(initialNode, variableInstruction, dstPDGNode, visitedNodes, loop);
                } else if (initialNode.declaresLocalVariable(variableInstruction) && !initialNode.equals(dstPDGNode)) {
                    //create def-order data dependence edge
                    PDGDataDependence dataDependence = new PDGDataDependence(initialNode, dstPDGNode, variableInstruction, loop);
                    edges.add(dataDependence);
                }
            }
        }
    }
}
