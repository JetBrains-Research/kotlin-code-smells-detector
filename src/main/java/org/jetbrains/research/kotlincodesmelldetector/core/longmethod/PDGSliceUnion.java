package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.declarations.FirFunction;
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeKind;

import java.util.*;

public class PDGSliceUnion {
    private final ControlFlowGraph cfg;
    private final HashSet<CFGNode<?>> sliceNodes;
    private final FirSimpleFunction function;
    private final FirVariable<?> localVariableCriterion;

    // now it just contains all graph's edges
    private final Map<CFGNode<?>, EdgeKind> edges = new HashMap<>();

    public PDGSliceUnion(FirSimpleFunction firSimpleFunction, ControlFlowGraph cfg, BasicBlock basicBlock, Set<CFGNode<?>> nodeCriteria, FirVariable<?> localVariableCriterion) {
        this.cfg = cfg;
        this.function = firSimpleFunction;
        this.sliceNodes = new HashSet<>();
        this.localVariableCriterion = localVariableCriterion;
        for (CFGNode<?> nodeCriterion : nodeCriteria) {
            sliceNodes.addAll(computeSlice(nodeCriterion));
        }


        //add any required object-state slices that may be used from the resulting slice
        // TODO omit it for now
//        Set<PDGNode> nodesToBeAddedToSliceDueToDependenceOnObjectStateSlices = new TreeSet<>();
//        Set<PlainVariable> alreadyExaminedObjectReferences = new LinkedHashSet<>();
//        for (PDGNode sliceNode : sliceNodes) {
//            Set<AbstractVariable> usedVariables = sliceNode.usedVariables;
//            for (AbstractVariable usedVariable : usedVariables) {
//                if (usedVariable instanceof PlainVariable) {
//                    PlainVariable plainVariable = (PlainVariable) usedVariable;
//                    if (!alreadyExaminedObjectReferences.contains(plainVariable)
//                            && !localVariableCriterion.getInitialVariable().equals(plainVariable)) {
//                        Map<CompositeVariable, LinkedHashSet<PDGNode>> definedAttributeNodeCriteriaMap =
//                                pdg.getDefinedAttributesOfReference(plainVariable);
//                        if (!definedAttributeNodeCriteriaMap.isEmpty()) {
//                            TreeSet<PDGNode> objectSlice = new TreeSet<>();
//                            for (CompositeVariable compositeVariable : definedAttributeNodeCriteriaMap.keySet()) {
//                                Set<PDGNode> nodeCriteria2 = definedAttributeNodeCriteriaMap.get(compositeVariable);
//                                for (PDGNode nodeCriterion : nodeCriteria2) {
//                                    if (subgraph.nodeBelongsToBlockBasedRegion(nodeCriterion))
//                                        objectSlice.addAll(subgraph.computeSlice(nodeCriterion));
//                                }
//                            }
//                            nodesToBeAddedToSliceDueToDependenceOnObjectStateSlices.addAll(objectSlice);
//                        }
//                        alreadyExaminedObjectReferences.add(plainVariable);
//                    }
//                }
//            }
//        }
//        sliceNodes.addAll(nodesToBeAddedToSliceDueToDependenceOnObjectStateSlices);
//        Set<PDGNode> throwStatementNodes = getThrowStatementNodesWithinRegion();
//        Set<PDGNode> nodesToBeAddedToSliceDueToThrowStatementNodes = new TreeSet<>();
//        for (PDGNode throwNode : throwStatementNodes) {
//            for (PDGNode sliceNode : sliceNodes) {
//                if (sliceNode instanceof PDGControlPredicateNode && isNestedInside(throwNode, sliceNode)) {
//                    Set<PDGNode> throwNodeSlice = subgraph.computeSlice(throwNode);
//                    nodesToBeAddedToSliceDueToThrowStatementNodes.addAll(throwNodeSlice);
//                    break;
//                }
//            }
//        }
//        sliceNodes.addAll(nodesToBeAddedToSliceDueToThrowStatementNodes);
//        Set<PDGNode> remainingNodes = new TreeSet<>();
//        remainingNodes.add(pdg.getEntryNode());
//        for (GraphNode node : pdg.nodes) {
//            PDGNode pdgNode = (PDGNode) node;
//            if (!sliceNodes.contains(pdgNode))
//                remainingNodes.add(pdgNode);
//        }
//        Set<PDGNode> throwStatementNodesToBeAddedToDuplicatedNodesDueToRemainingNodes = new TreeSet<>();
//        for (PDGNode throwNode : throwStatementNodes) {
//            for (PDGNode remainingNode : remainingNodes) {
//                if (remainingNode.getId() != 0 && isNestedInside(throwNode, remainingNode)) {
//                    throwStatementNodesToBeAddedToDuplicatedNodesDueToRemainingNodes.add(throwNode);
//                    break;
//                }
//            }
//        }
//        this.passedParameters = new LinkedHashSet<>();
//        Set<PDGNode> nCD = new LinkedHashSet<>();
//        Set<PDGNode> nDD = new LinkedHashSet<>();
//        for (GraphEdge edge : pdg.edges) {
//            PDGDependence dependence = (PDGDependence) edge;
//            PDGNode srcPDGNode = (PDGNode) dependence.src;
//            PDGNode dstPDGNode = (PDGNode) dependence.dst;
//            if (dependence instanceof PDGDataDependence) {
//                PDGDataDependence dataDependence = (PDGDataDependence) dependence;
//                if (remainingNodes.contains(srcPDGNode) && sliceNodes.contains(dstPDGNode))
//                    passedParameters.add(dataDependence.getData());
//                if (sliceNodes.contains(srcPDGNode) && remainingNodes.contains(dstPDGNode)
//                        && !dataDependence.getData().equals(localVariableCriterion)
//                        && !dataDependence.getData().isField())
//                    nDD.add(srcPDGNode);
//            } else if (dependence instanceof PDGControlDependence) {
//                if (sliceNodes.contains(srcPDGNode) && remainingNodes.contains(dstPDGNode))
//                    nCD.add(srcPDGNode);
//            }
//        }
//        Set<PDGNode> controlIndispensableNodes = new LinkedHashSet<>();
//        for (PDGNode p : nCD) {
//            for (AbstractVariable usedVariable : p.usedVariables) {
//                Set<PDGNode> pSliceNodes = subgraph.computeSlice(p, usedVariable);
//                for (GraphNode node : pdg.nodes) {
//                    PDGNode q = (PDGNode) node;
//                    if (pSliceNodes.contains(q) || q.equals(p))
//                        controlIndispensableNodes.add(q);
//                }
//            }
//            if (p.usedVariables.isEmpty()) {
//                Set<PDGNode> pSliceNodes = subgraph.computeSlice(p);
//                for (GraphNode node : pdg.nodes) {
//                    PDGNode q = (PDGNode) node;
//                    if (pSliceNodes.contains(q) || q.equals(p))
//                        controlIndispensableNodes.add(q);
//                }
//            }
//        }
//        Set<PDGNode> dataIndispensableNodes = new LinkedHashSet<>();
//        for (PDGNode p : nDD) {
//            for (AbstractVariable definedVariable : p.definedVariables) {
//                Set<PDGNode> pSliceNodes = subgraph.computeSlice(p, definedVariable);
//                for (GraphNode node : pdg.nodes) {
//                    PDGNode q = (PDGNode) node;
//                    if (pSliceNodes.contains(q))
//                        dataIndispensableNodes.add(q);
//                }
//            }
//        }
//        this.indispensableNodes = new TreeSet<>();
//        indispensableNodes.addAll(controlIndispensableNodes);
//        indispensableNodes.addAll(dataIndispensableNodes);
//        Set<PDGNode> throwStatementNodesToBeAddedToDuplicatedNodesDueToIndispensableNodes = new TreeSet<>();
//        for (PDGNode throwNode : throwStatementNodes) {
//            for (PDGNode indispensableNode : indispensableNodes) {
//                if (isNestedInside(throwNode, indispensableNode)) {
//                    throwStatementNodesToBeAddedToDuplicatedNodesDueToIndispensableNodes.add(throwNode);
//                    break;
//                }
//            }
//        }
//        for (PDGNode throwNode : throwStatementNodesToBeAddedToDuplicatedNodesDueToRemainingNodes) {
//            indispensableNodes.addAll(subgraph.computeSlice(throwNode));
//        }
//        for (PDGNode throwNode : throwStatementNodesToBeAddedToDuplicatedNodesDueToIndispensableNodes) {
//            indispensableNodes.addAll(subgraph.computeSlice(throwNode));
//        }
//        this.removableNodes = new LinkedHashSet<>();
//        for (GraphNode node : pdg.nodes) {
//            PDGNode pdgNode = (PDGNode) node;
//            if (!remainingNodes.contains(pdgNode) && !indispensableNodes.contains(pdgNode))
//                removableNodes.add(pdgNode);
//        }
    }

    private Collection<? extends CFGNode<?>> computeSlice(CFGNode<?> nodeCriterion) {
        return new LinkedHashSet<>(traverseBackward(nodeCriterion, new LinkedHashSet<>()));
    }

    private Set<CFGNode<?>> traverseBackward(CFGNode<?> node, Set<CFGNode<?>> visitedNodes) {
        Set<CFGNode<?>> sliceNodes = new LinkedHashSet<>();
        sliceNodes.add(node);
        visitedNodes.add(node);
        for (Map.Entry<CFGNode<?>, EdgeKind> edge : node.getIncomingEdges().entrySet()) {
            // TODO make condition full
//            if (edges.contains(dependence) && !(dependence instanceof PDGAntiDependence)
//                    && !(dependence instanceof PDGOutputDependence)) {
            CFGNode<?> sourceNode = edge.getKey();
            EdgeKind edgeKind = edge.getValue();
            // check if the cfg contains such a node
            if (edges.containsKey(sourceNode) && edges.get(sourceNode) == edgeKind) {
                 CFGNode<?> srcPDGNode = edge.getKey();
                if (!visitedNodes.contains(srcPDGNode))
                    sliceNodes.addAll(traverseBackward(srcPDGNode, visitedNodes));
            }
        }
        return sliceNodes;
    }

    public Set<CFGNode<?>> getSliceNodes() {
        return sliceNodes;
    }

    public FirSimpleFunction getFunction() {
        return function;
    }

    public FirVariable<?> getLocalVariableCriterion() {
        return localVariableCriterion;
    }
}
