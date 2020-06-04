package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirProperty;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment;
import org.jetbrains.kotlin.fir.references.FirNamedReference;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode;
import org.jetbrains.kotlin.fir.visitors.FirVisitor;
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid;
import org.jetbrains.research.kotlincodesmelldetector.utils.FirUtilsKt;

import java.util.*;

public class PDGNode {
    private static int nodeNum = 0;
    private CFGNode<?> cfgNode;
    private List<FirVariable<?>> variableDeclarationsInMethod;
    protected int id;
    protected final Set<GraphEdge> incomingEdges;
    protected final Set<GraphEdge> outgoingEdges;

    PDGNode() {
        nodeNum++;
        this.id = nodeNum;
        this.incomingEdges = new LinkedHashSet<>();
        this.outgoingEdges = new LinkedHashSet<>();
    }

    PDGNode(CFGNode<?> cfgNode, List<FirVariable<?>> variableDeclarationsInMethod, Map<CFGNode<?>, PDGNode> cfgToPdgNode) {
        this();
        this.cfgNode = cfgNode;
        this.variableDeclarationsInMethod = variableDeclarationsInMethod;
        cfgToPdgNode.put(cfgNode, this);
    }

    public int getId() {
        return id;
    }

    void addIncomingEdge(GraphEdge edge) {
        incomingEdges.add(edge);
    }

    void addOutgoingEdge(GraphEdge edge) {
        outgoingEdges.add(edge);
    }

    static void resetNodeNum() {
        nodeNum = 0;
    }

    public CFGNode<?> getCfgNode() {
        return cfgNode;
    }

    public List<FirProperty> definedVariables() {
        // todo add firVariableAssignment
        if (this.getCfgNode() instanceof VariableDeclarationNode) {
            VariableDeclarationNode declarationNode = (VariableDeclarationNode) this.getCfgNode();
            return Collections.singletonList(declarationNode.getFir());
        }
        return Collections.emptyList();
    }

    public boolean definesLocalVariable(FirVariable<?> variableInstruction) {
        return FirUtilsKt.definesLocalVariable(cfgNode.getFir(), variableInstruction);
    }

    public boolean declaresLocalVariable(FirVariable<?> variableInstruction) {
        return FirUtilsKt.declaresLocalVariable(cfgNode.getFir(), variableInstruction);

    }

    public boolean usesLocalVariable(FirVariable<?> variableInstruction) {
        return FirUtilsKt.usesLocalVariable(cfgNode.getFir(), variableInstruction);
    }

    public static class GraphEdge {
        protected final PDGNode src;
        protected final PDGNode dst;

        GraphEdge(PDGNode src, PDGNode dst) {
            this.src = src;
            this.dst = dst;
        }

        public PDGNode getSrc() {
            return src;
        }

        public PDGNode getDst() {
            return dst;
        }
    }
}