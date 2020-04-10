package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import com.intellij.psi.*;
import com.sun.istack.NotNull;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.kotlin.psi.KtFunction;

import java.util.*;


public class ASTSlice {
    @NotNull
    private SmartPsiElementPointer<PsiElement> sourceTypeDeclaration;
    @NotNull
    private SmartPsiElementPointer<PsiElement> sourceMethodDeclaration;
    @NotNull
    private SmartPsiElementPointer<PsiElement> psiFile;
    @NotNull
    private SmartPsiElementPointer<PsiElement> variableCriterionDeclarationStatement;
    @NotNull
    private SmartPsiElementPointer<PsiElement> extractedMethodInvocationInsertionStatement;
    private SmartPsiElementPointer<PsiElement> localVariableCriterion;
    private Set<PDGNode> sliceNodes;
    private Set<SmartPsiElementPointer<PsiElement>> sliceStatements;
    private Set<SmartPsiElementPointer<PsiElement>> removableStatements;
    private Set<SmartPsiElementPointer<PsiElement>> duplicatedStatements;
    private Set<SmartPsiElementPointer<PsiElement>> passedParameters;

    private String extractedMethodName;
    private boolean declarationOfVariableCriterionBelongsToSliceNodes;
    private boolean declarationOfVariableCriterionBelongsToRemovableNodes;
    private BasicBlock boundaryBlock;
    private boolean isObjectSlice;
    private int methodSize;

    // TODO no constructors here
    private int depthOfNesting(PsiStatement statement) {
        int depthOfNesting = 0;
        PsiElement parent = statement;
        while (!(parent instanceof PsiMethod)) {
            depthOfNesting++;
            parent = parent.getParent();
        }
        return depthOfNesting;
    }

    public PsiClass getSourceTypeDeclaration() {
        return (PsiClass) sourceTypeDeclaration.getElement();
    }

    // TODO change it
    public KtFunction getSourceMethodDeclarationKt() {
        return (KtFunction) sourceMethodDeclaration.getElement();
    }

    public PsiMethod getSourceMethodDeclaration() {
        return (PsiMethod) sourceMethodDeclaration.getElement();
    }

    public PsiVariable getLocalVariableCriterion() {
        return (PsiVariable) localVariableCriterion.getElement();
    }

    public Set<SmartPsiElementPointer<PsiElement>> getPassedParameters() {
        return passedParameters;
    }

    // TODO implement
    public Set<CFGNode<? extends FirElement>> getCfgSliceNodes() {
        return new HashSet<>();
    }

    public Set<PDGNode> getSliceNodes() {
        return sliceNodes;
    }

    public Set<SmartPsiElementPointer<PsiElement>> getSliceStatements() {
        return sliceStatements;
    }

    private Set<SmartPsiElementPointer<PsiElement>> getRemovableStatements() {
        return removableStatements;
    }

    private PsiStatement getVariableCriterionDeclarationStatement() {
        return variableCriterionDeclarationStatement == null ? null : (PsiStatement) variableCriterionDeclarationStatement.getElement();
    }

    private PsiStatement getExtractedMethodInvocationInsertionStatement() {
        return (PsiStatement) extractedMethodInvocationInsertionStatement.getElement();
    }

    public String toString() {
        return getSourceTypeDeclaration().getQualifiedName() + "::" +
                getSourceMethodDeclaration().getName();
    }

    /**
     * Checks all {@link PsiStatement} from slice for availability.
     *
     * @return true if all {@link PsiStatement} are valid, false otherwise.
     */
    public boolean areSliceStatementsValid() {
        for (SmartPsiElementPointer<PsiElement> psiElementSmartPsiElementPointer : this.getSliceStatements()) {
            if (psiElementSmartPsiElementPointer.getElement() == null ||
                    psiElementSmartPsiElementPointer.getElement() instanceof PsiStatement
                            && !psiElementSmartPsiElementPointer.getElement().isValid()) {
                return false;
            }
        }
        return true;
    }

}
