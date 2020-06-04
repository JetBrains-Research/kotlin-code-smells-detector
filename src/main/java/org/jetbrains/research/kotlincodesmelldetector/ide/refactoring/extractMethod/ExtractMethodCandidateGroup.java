package org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractMethod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.ASTSlice;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.Refactoring;

import java.util.Optional;
import java.util.Set;

/**
 * Representation of a refactoring, which suggests to extract code into separate method.
 */
public class ExtractMethodCandidateGroup implements Refactoring {
    private final @NotNull FirSimpleFunction method;
    private @NotNull final Set<ASTSlice> candidates;

    /**
     * Creates refactoring instance.
     *
     * @param slices slice group that consist of candidates to extract.
     */
    public ExtractMethodCandidateGroup(Set<ASTSlice> slices) {
        this.method = slices.iterator().next().getSourceMethodDeclaration();
        this.candidates = slices;
    }

    /**
     * Returns a method from which code is proposed to be extracted into a separate method.
     */
    public @NotNull
    FirSimpleFunction getMethod() {
        return Optional.ofNullable(method).orElseThrow(() ->
                                                               new IllegalStateException("Cannot get method. Reference is invalid."));
    }

    /**
     * Returns a method that is proposed to be moved in this refactoring.
     */
    public @NotNull
    Optional<FirSimpleFunction> getOptionalMethod() {
        return Optional.ofNullable(method);
    }

    @NotNull
    public Set<ASTSlice> getCandidates() {
        return candidates;
    }

    @NotNull
    @Override
    public String getDescription() {
        Optional<FirSimpleFunction> method = getOptionalMethod();
        return method.map(psiMethod ->
                                  String.join(DELIMITER, toString(), // TODO calculate signature
                                              candidates.iterator().next().getLocalVariableCriterion().getName().toString())).orElse("");
    }

    @NotNull
    @Override
    public String getExportDefaultFilename() {
        return "Long-Method";
    }

    @Override
    // TODO add enclosing class if any
    public String toString() {
        FirSimpleFunction function = getMethod();
        return function.getName().toString();
    }
}
