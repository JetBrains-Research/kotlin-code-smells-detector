package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import org.jetbrains.kotlin.psi.KtElement;

import java.util.HashSet;
import java.util.Set;

class DistanceCalculator {

    public static double getDistance(Set<KtElement> set1, Set<KtElement> set2) {
        if (set1.isEmpty() && set2.isEmpty())
            return 1.0;
        return 1.0 - (double) intersection(set1, set2).size() / (double) union(set1, set2).size();
    }

    private static Set<KtElement> union(Set<KtElement> set1, Set<KtElement> set2) {
        Set<KtElement> set = new HashSet<>();
        set.addAll(set1);
        set.addAll(set2);
        return set;
    }

    public static Set<KtElement> intersection(Set<KtElement> set1, Set<KtElement> set2) {
        Set<KtElement> set = new HashSet<>(set1);
        set.retainAll(set2);
        return set;
    }
}
