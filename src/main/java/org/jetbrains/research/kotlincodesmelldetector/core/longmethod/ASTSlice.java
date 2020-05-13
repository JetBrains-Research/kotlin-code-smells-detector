package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import com.sun.istack.NotNull;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirFunction;
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.research.kotlincodesmelldetector.utils.FirUtilsKt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ASTSlice {
    // @NotNull
    //  private final FirClass<?> sourceTypeDeclaration;
    @NotNull
    private final FirSimpleFunction sourceMethodDeclaration;
    private final List<CFGNode<?>> sliceNodes;
    private final Set<FirElement> sliceStatements;
    private FirVariable<?> localVariableCriterion;

    public ASTSlice(PDGSliceUnion sliceUnion) {
        this.sourceMethodDeclaration = sliceUnion.getFunction();
        // TODO add this later
        // this.sourceTypeDeclaration = sourceMethodDeclaration.
        this.sliceNodes = sliceUnion.getSliceNodes();
        this.sliceStatements = new LinkedHashSet<>();
        for (CFGNode<?> node : sliceNodes) {
            sliceStatements.add(node.getFir());
        }

        // TODO no accessed fields for now
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
//
//    public FirClass<?> getSourceTypeDeclaration() {
//        return sourceTypeDeclaration;
//    }

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

    // TODO find a better way than calling toString on them?
    public String toString() {
//        return getSourceTypeDeclaration().toString() + "::" +
//                getSourceMethodDeclaration().toString();
        return getSourceMethodDeclaration().toString();
    }

    // TODO make correct
    public static boolean areSliceStatementsValid(ASTSlice astSlice) {
        return true;
    }

    // TODO make correct
    public static boolean areSliceStatementsValid() {
        return true;
    }



    // TODO do we need the analog of it?
    //    /**
    //     * Checks all {@link PsiStatement} from slice for availability.
    //     *
    //     * @return true if all {@link PsiStatement} are valid, false otherwise.
    //     */
    //    public boolean areSliceStatementsValid() {
    //        for (SmartPsiElementPointer<PsiElement> psiElementSmartPsiElementPointer : this.getSliceStatements()) {
    //            if (psiElementSmartPsiElementPointer.getElement() == null ||
    //                    psiElementSmartPsiElementPointer.getElement() instanceof PsiStatement
    //                            && !psiElementSmartPsiElementPointer.getElement().isValid()) {
    //                return false;
    //            }
    //        }
    //        return true;
    //    }
}
