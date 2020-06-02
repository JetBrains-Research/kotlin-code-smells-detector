package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirDeclaration;
import org.jetbrains.kotlin.fir.declarations.FirProperty;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment;
import org.jetbrains.kotlin.fir.references.FirNamedReference;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode;
import org.jetbrains.kotlin.fir.visitors.FirVisitor;

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
        // add firvariableassignment?
        if (this.getCfgNode() instanceof VariableDeclarationNode) {
            VariableDeclarationNode declarationNode = (VariableDeclarationNode) this.getCfgNode();
            return Collections.singletonList(declarationNode.getFir());
        }
        return Collections.emptyList();
    }

    // todo add assignment
    public boolean definesLocalVariable(FirVariable<?> variableInstruction) {
        List<FirVariable<?>> data = new ArrayList<>();
        this.cfgNode.getFir().accept(new FirVisitor<Void, List<FirVariable<?>>>() {
            @Override
            public Void visitVariableAssignment(@NotNull FirVariableAssignment variableAssignment, List<FirVariable<?>> data) {
                if (variableAssignment.getLValue() instanceof FirNamedReference) {
                    FirNamedReference lValue = (FirNamedReference) variableAssignment.getLValue();
                    if (lValue.getName().equals(variableInstruction.getName())) {
                        data.add(variableInstruction);
                    }
                }
                return null;
            }

            @Override
            public Void visitProperty(@NotNull FirProperty property, List<FirVariable<?>> data) {
                if (property.equals(variableInstruction)) {
                    data.add(property);
                }
                return null;
            }

            @Override
            public Void visitElement(@NotNull FirElement firElement, List<FirVariable<?>> firVariables) {
                return null;
            }
        }, data);
        return !data.isEmpty();
    }

    public boolean declaresLocalVariable(FirVariable<?> variableInstruction) {
        List<FirVariable<?>> data = new ArrayList<>();
        this.cfgNode.getFir().accept(new FirVisitor<Void, List<FirVariable<?>>>() {
            @Override
            public Void visitProperty(@NotNull FirProperty property, List<FirVariable<?>> data) {
                if (property.equals(variableInstruction)) {
                    data.add(property);
                }
                return null;
            }

            @Override
            public Void visitElement(@NotNull FirElement firElement, List<FirVariable<?>> firVariables) {
                return null;
            }
        }, data);
        return !data.isEmpty();
    }

    public boolean usesLocalVariable(FirVariable<?> variableInstruction) {
        List<FirVariable<?>> data = new ArrayList<>();
        this.cfgNode.getFir().accept(new FirVisitor<Void, List<FirVariable<?>>>() {
            @Override
            public Void visitElement(@NotNull FirElement firElement, List<FirVariable<?>> firVariables) {
                System.out.println(firElement);
                return null;
            }

            @Override
            public Void visitProperty(@NotNull FirProperty property, List<FirVariable<?>> data) {
                if (property.equals(variableInstruction)) {
                    data.add(property);
                }
                System.out.println("AAAA");
                return null;
            }


        }, data);
        return !data.isEmpty();
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