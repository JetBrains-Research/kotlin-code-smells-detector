package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph;

import java.util.*;

public class PDGSliceUnion {
    private final ControlFlowGraph cfg;
    private final List<CFGNode<?>> sliceNodes;
    private final FirSimpleFunction function;
    private final FirVariable<?> localVariableCriterion;

    public PDGSliceUnion(FirSimpleFunction firSimpleFunction, ControlFlowGraph cfg, BasicBlock basicBlock, Set<CFGNode<?>> nodeCriteria, FirVariable<?> localVariableCriterion) {
        this.cfg = cfg;
        this.function = firSimpleFunction;
        this.sliceNodes = new ArrayList<>();
        this.localVariableCriterion = localVariableCriterion;
        for (CFGNode<?> nodeCriterion : nodeCriteria) {
            sliceNodes.addAll(computeSlice(nodeCriterion));
        }
        // TODO handle object-state slices
    }

    private Collection<? extends CFGNode<?>> computeSlice(CFGNode<?> nodeCriterion) {
        return new LinkedHashSet<>(traverseBackward(nodeCriterion, new LinkedHashSet<>()));
    }

    private Set<CFGNode<?>> traverseBackward(CFGNode<?> node, Set<CFGNode<?>> visitedNodes) {
        Set<CFGNode<?>> sliceNodes = new LinkedHashSet<>();
        sliceNodes.add(node);
        visitedNodes.add(node);
        for (CFGNode<?> previousNode : node.getPreviousNodes()) {
            // TODO filter out special cases
            if (!visitedNodes.contains(previousNode)) {
                sliceNodes.addAll(traverseBackward(previousNode, visitedNodes));
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
