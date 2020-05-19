package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import com.sun.istack.NotNull;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.research.kotlincodesmelldetector.utils.FirUtilsKt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ASTSlice {
    @NotNull private final FirSimpleFunction sourceMethodDeclaration;
    private final List<CFGNode<?>> sliceNodes;
    private final Set<FirElement> sliceStatements;
    private FirVariable<?> localVariableCriterion;

    public ASTSlice(PDGSliceUnion sliceUnion) {
        this.sourceMethodDeclaration = sliceUnion.getFunction();
        this.sliceNodes = sliceUnion.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (CFGNode<?> node : sliceNodes) {
            sliceStatements.add(node.getFir());
        }

        // TODO handle accessed fields
        // Set<PsiVariable> variableDeclarationsAndAccessedFields = sliceUnion.getVariableDeclarationsAndAccessedFieldsInMethod();
        List<FirVariable<?>> variableDeclarationsInFunction = FirUtilsKt.getVariableDeclarationsInFunction(sliceUnion.getFunction());
        FirVariable<?> criterion = sliceUnion.getLocalVariableCriterion();
        for (FirVariable<?> variableDeclaration : variableDeclarationsInFunction) {
            if (variableDeclaration.equals(criterion)) {
                this.localVariableCriterion = variableDeclaration;
                break;
            }
        }
    }

    // TODO implement
    public static boolean areSliceStatementsValid(ASTSlice astSlice) {
        return true;
    }

    // TODO implement
    public static boolean areSliceStatementsValid() {
        return true;
    }

    public FirSimpleFunction getSourceMethodDeclaration() {
        return sourceMethodDeclaration;
    }

    public FirVariable<?> getLocalVariableCriterion() {
        return localVariableCriterion;
    }

    public List<CFGNode<? extends FirElement>> getSliceNodes() {
        return sliceNodes;
    }

    public Set<FirElement> getSliceStatements() {
        return sliceStatements;
    }

    // TODO add enclosing class name if any
    public String toString() {
        return getSourceMethodDeclaration().getName().toString();
    }
}
