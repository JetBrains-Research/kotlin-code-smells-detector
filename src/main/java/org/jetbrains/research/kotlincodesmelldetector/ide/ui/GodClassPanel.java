package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.RefactoringType.AbstractCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassRefactoring;

import java.util.Collections;

/**
 * Panel for God Class Checking refactorings.
 */
public class GodClassPanel extends AbstractRefactoringPanel {
    private static final String[] COLUMN_NAMES = new String[]{KotlinCodeSmellDetectorBundle.message("god.class.panel.source.class"),
            KotlinCodeSmellDetectorBundle.message("god.class.panel.extractable.concept"),
            KotlinCodeSmellDetectorBundle.message("god.class.panel.source.extracted.members")};

    private static final int REFACTOR_DEPTH = 4;

    public GodClassPanel(@NotNull Project project) {
        super(project,
                "god.class.identification.indicator",
                new ExtractClassRefactoringType(),
                new GodClassTreeTableModel(Collections.emptyList(), COLUMN_NAMES),
                REFACTOR_DEPTH);
    }

    @Override
    protected void doRefactor(AbstractCandidateRefactoring candidateRefactoring) {
        AbstractExtractClassRefactoring abstractRefactoring = (AbstractExtractClassRefactoring) getAbstractRefactoringFromAbstractCandidateRefactoring(candidateRefactoring);

        TransactionGuard.getInstance().submitTransactionAndWait(() -> {
            removeHighlighters(project);
            GodClassUserInputDialog dialog = new GodClassUserInputDialog(abstractRefactoring.getRefactoring());
            dialog.show();
            if (dialog.isOK()) {
                showRefreshingProposal();
            }
        });
    }
}
