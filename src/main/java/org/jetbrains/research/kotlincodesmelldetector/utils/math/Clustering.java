package org.jetbrains.research.kotlincodesmelldetector.utils.math;


import org.jetbrains.kotlin.psi.KtDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class Clustering {

    protected ArrayList<ArrayList<Double>> distanceList;
    protected double[][] distanceMatrix;

    public static Clustering getInstance(int type, double[][] distanceMatrix) {
        if (type == 0) {
            return new Hierarchical(distanceMatrix);
        }
        return null;
    }

    public abstract HashSet<Cluster> clustering(List<? extends KtDeclaration> entities);
}
