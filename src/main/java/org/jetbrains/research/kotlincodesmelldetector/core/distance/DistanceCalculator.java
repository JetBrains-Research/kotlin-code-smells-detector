package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import java.util.HashSet;
import java.util.Set;

class DistanceCalculator {

    public static double getDistance(Set<Entity> set1, Set<Entity> set2) {
        if (set1.isEmpty() && set2.isEmpty())
            return 1.0;
        return 1.0 - (double) intersection(set1, set2).size() / (double) union(set1, set2).size();
    }

    private static Set<Entity> union(Set<Entity> set1, Set<Entity> set2) {
        Set<Entity> set = new HashSet<>();
        set.addAll(set1);
        set.addAll(set2);
        return set;
    }

    public static Set<Entity> intersection(Set<Entity> set1, Set<Entity> set2) {
        Set<Entity> set = new HashSet<>(set1);
        set.retainAll(set2);
        return set;
    }
}
