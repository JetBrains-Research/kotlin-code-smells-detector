package org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateGroup;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.RefactoringType;
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.AbstractRefactoringPanel;

import java.util.List;
import java.util.Set;

import static org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellFacade.getExtractClassRefactoringOpportunities;

public class ExtractClassRefactoringType extends RefactoringType {
    @Override
    public AbstractCandidateRefactoring newCandidateRefactoring(Object candidateRefactoring) {
        return new AbstractExtractClassCandidateRefactoring((ExtractClassCandidateRefactoring) candidateRefactoring);
    }

    @Override
    public boolean instanceOfCandidateRefactoring(Object o) {
        return o instanceof AbstractExtractClassCandidateRefactoring;
    }

    @Override
    public Set<?> getNotAbstractRefactoringOpportunities(ProjectInfo projectInfo, ProgressIndicator indicator, long[] time) {
        return getExtractClassRefactoringOpportunities(projectInfo, indicator, time);
    }

    @Override
    public AbstractRefactoring newAbstractRefactoring(AbstractCandidateRefactoring candidateRefactoring) {
        return new AbstractExtractClassRefactoring((ExtractClassCandidateRefactoring) candidateRefactoring.getCandidateRefactoring());
    }

    @Override
    public AbstractCandidateRefactoringGroup newAbstractCandidateRefactoringGroup(Object candidateRefactoringGroup) {
        return new AbstractExtractClassCandidateRefactoringGroup(candidateRefactoringGroup);
    }

    public static class AbstractExtractClassCandidateRefactoring extends AbstractCandidateRefactoring {
        private final SmartPsiElementPointer<KtElement> sourceClass;

        public AbstractExtractClassCandidateRefactoring(ExtractClassCandidateRefactoring candidateRefactoring) {
            super(candidateRefactoring);
            this.sourceClass = candidateRefactoring.getSourceClass();
        }

        @Override
        public KtElement getSourceClass() {
            return sourceClass.getElement();
        }

        @Override
        public void highlightCode() {
            ExtractClassCandidateRefactoring refactoring = (ExtractClassCandidateRefactoring) getCandidateRefactoring();

            AbstractRefactoringPanel.removeHighlighters(getSourceClass().getProject());

            boolean openInEditor = true; //open only first element

            for (KtDeclaration method : refactoring.getExtractedMethods()) {
                AbstractRefactoringPanel.highlightMethod(method, new AnalysisScope(method.getProject()), openInEditor);
                openInEditor = false;
            }

            for (KtDeclaration field : refactoring.getExtractedFields()) {
                AbstractRefactoringPanel.highlightProperty(field, new AnalysisScope(field.getProject()), openInEditor);
                openInEditor = false;
            }
        }

        @Override
        public String toString() {
            return ((ExtractClassCandidateRefactoring) getCandidateRefactoring()).getTopics().toString();
        }

        @NotNull
        @Override
        public String getDescription() {
            return candidateRefactoring.toString();
        }

        @NotNull
        @Override
        public String getExportDefaultFilename() {
            return "God-Class";
        }
    }

    public class AbstractExtractClassCandidateRefactoringGroup extends AbstractCandidateRefactoringGroup {
        public AbstractExtractClassCandidateRefactoringGroup(Object candidateRefactoringGroup) {
            super(ExtractClassRefactoringType.this, candidateRefactoringGroup);
        }

        @Override
        protected List<?> getNotAbstractCandidates() {
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) getCandidateRefactoringGroup();
            return group.getCandidates();
        }

        @Override
        public String toString() {
            return ((ExtractClassCandidateGroup) getCandidateRefactoringGroup()).getSource().getElement().getName();
        }
    }

    public static class AbstractExtractClassRefactoring extends AbstractRefactoring {
        private final ExtractClassRefactoring refactoring;

        public AbstractExtractClassRefactoring(ExtractClassCandidateRefactoring extractClassCandidateRefactoring) {
            this.refactoring = new ExtractClassRefactoring(extractClassCandidateRefactoring.getSourceFile(),
                    extractClassCandidateRefactoring.getSourceClass().getElement(),
                    extractClassCandidateRefactoring.getExtractedFields(),
                    extractClassCandidateRefactoring.getExtractedMethods(),
                    extractClassCandidateRefactoring.getDelegateFunctions(),
                    extractClassCandidateRefactoring.getDefaultTargetClassName());
        }

        @Override
        public void apply() {
            refactoring.apply();
        }

        public ExtractClassRefactoring getRefactoring() {
            return refactoring;
        }
    }
}
