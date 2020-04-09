package org.jetbrains.research.kotlincodesmelldetector.core.distance

import java.util.Comparator

class ClusterSizeComparator : Comparator<ExtractClassCandidateRefactoring> {
    override fun compare(
        o1: ExtractClassCandidateRefactoring,
        o2: ExtractClassCandidateRefactoring
    ): Int {
        return o2.extractedEntities.size.compareTo(o1.extractedEntities.size)
    }
}
