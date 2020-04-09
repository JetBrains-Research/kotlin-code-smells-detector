package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

class DistanceCalculator {

    public static double getDistance(@NotNull Set<PsiElement> set1, @NotNull Set<PsiElement> set2) {
        if (set1.isEmpty() && set2.isEmpty())
            return 1.0;
        return 1.0 - (double) intersection(set1, set2).size() / (double) union(set1, set2).size();
    }

    private static Set<PsiElement> union(Set<PsiElement> set1, Set<PsiElement> set2) {
        Set<PsiElement> set = new HashSet<>();
        set.addAll(set1);
        set.addAll(set2);
        return set;
    }

    public static Set<PsiElement> intersection(Set<PsiElement> set1, Set<PsiElement> set2) {
        Set<PsiElement> set = new HashSet<>(set1);
        set.retainAll(set2);
        return set;
    }
}
