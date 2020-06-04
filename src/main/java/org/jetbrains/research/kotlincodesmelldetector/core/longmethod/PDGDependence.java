package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

public abstract class PDGDependence extends PDGNode.GraphEdge {
    public enum PDGDependenceType {
        CONTROL,
        DATA,
        ANTI,
        OUTPUT
    }

    private final PDGDependenceType type;

    PDGDependence(PDGNode src, PDGNode dst, PDGDependenceType type) {
        super(src, dst);
        this.type = type;
    }

    public PDGNode getSrc() {
        return src;
    }

    public PDGNode getDst() {
        return dst;
    }

    PDGDependenceType getType() {
        return type;
    }
}
