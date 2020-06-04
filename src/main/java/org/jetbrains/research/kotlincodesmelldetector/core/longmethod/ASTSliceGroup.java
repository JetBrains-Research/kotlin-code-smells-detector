package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import java.util.LinkedHashSet;
import java.util.Set;

public class ASTSliceGroup {
    private final LinkedHashSet<ASTSlice> candidates;

    public ASTSliceGroup() {
        this.candidates = new LinkedHashSet<>();
    }

    public void addCandidate(ASTSlice slice) {
        // TODO enable this filtering
//        LinkedHashSet<ASTSlice> slicesToBeRemoved = new LinkedHashSet<>();
//        for (ASTSlice previousSlice : candidates) {
//            if (previousSlice.getNumberOfSliceStatements() == slice.getNumberOfSliceStatements()
//                    && previousSlice.getNumberOfDuplicatedStatements() == slice.getNumberOfDuplicatedStatements()) {
//                slicesToBeRemoved.add(previousSlice);
//            }
//        }
//        this.candidates.removeAll(slicesToBeRemoved);
        this.candidates.add(slice);
    }

    public Set<ASTSlice> getCandidates() {
        return candidates;
    }
}
