package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo;
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.ASTSlice;
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.ASTSliceGroup;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractMethod.ExtractMethodCandidateGroup;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractMethod.MyExtractMethodProcessor;
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.listeners.DoubleClickListener;
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.listeners.ElementSelectionListener;
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.listeners.EnterKeyListener;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellFacade.getExtractMethodRefactoringOpportunities;
import static org.jetbrains.research.kotlincodesmelldetector.ide.ui.AbstractRefactoringPanel.expandOrCollapsePath;
import static org.jetbrains.research.kotlincodesmelldetector.ide.ui.AbstractRefactoringPanel.runAfterCompilationCheck;
import static org.jetbrains.research.kotlincodesmelldetector.utils.PsiUtils.isChild;

/**
 * Panel for Extract Method refactoring.
 */
class ExtractMethodPanel extends JPanel {
    @NotNull
    private final AnalysisScope scope;
    private final ExtractMethodTreeTableModel treeTableModel = new ExtractMethodTreeTableModel();
    private final TreeTable treeTable = new TreeTable(treeTableModel);
    private final JButton doRefactorButton = new JButton();
    private final JButton refreshButton = new JButton();
    private final JButton exportButton = new JButton();
    private final JLabel refreshLabel = new JLabel(
            KotlinCodeSmellDetectorBundle.message("press.refresh.to.find.refactoring.opportunities"),
            SwingConstants.CENTER
    );
    private JScrollPane scrollPane = new JBScrollPane();

    ExtractMethodPanel(@NotNull AnalysisScope scope) {
        this.scope = scope;
        setLayout(new BorderLayout());
        setupGUI();
    }

//    /**
//     * Opens definition of method and highlights statements, which should be extracted.
//     *
//     * @param sourceMethod method from which code is proposed to be extracted into separate method.
//     * @param scope        scope of the current project.
//     * @param slice        computation slice.
//     */
//    private static void openDefinition(@Nullable FirSimpleFunction sourceMethod, AnalysisScope scope, ASTSlice slice) {
//        new Task.Backgroundable(scope.getProject(), "Search Definition") {
//            @Override
//            public void run(@NotNull ProgressIndicator indicator) {
//                indicator.setIndeterminate(true);
//            }
//
//            @Override
//            public void onSuccess() {
//                if (sourceMethod != null) {
//                    Set<FirElement> statements = slice.getSliceStatements();
//                    FirElement psiStatement = statements.iterator().next();
//                    if (psiStatement != null && psiStatement.isValid()) {
//                        EditorHelper.openInEditor(psiStatement);
//                        Editor editor = FileEditorManager.getInstance(sourceMethod.getProject()).getSelectedTextEditor();
//                        if (editor != null) {
//                            TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
//                            editor.getMarkupModel().removeAllHighlighters();
//                            statements.stream()
//                                    .filter(statement -> statement != null)
//                                    .forEach(statement ->
//                                                     editor.getMarkupModel().addRangeHighlighter(statement.getTextRange().getStartOffset(),
//                                                                                                 statement.getTextRange().getEndOffset(), HighlighterLayer.SELECTION,
//                                                                                                 attributes, HighlighterTargetArea.EXACT_RANGE));
//                        }
//                    }
//                }
//            }
//        }.queue();
//    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    /**
     * Creates scrollable table panel and adds mouse listener.
     *
     * @return result panel.
     */
    private JScrollPane createTablePanel() {
        treeTable.setRootVisible(false);
        treeTable.setTreeCellRenderer(new ExtractMethodCandidatesTreeCellRenderer());
        treeTable.getColumnModel().getColumn(0).setPreferredWidth(800);
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //treeTable.addMouseListener((DoubleClickListener) this::openMethodDefinition);
       // treeTable.addKeyListener((EnterKeyListener) this::openMethodDefinition);
        treeTable.getTree().addTreeSelectionListener((ElementSelectionListener) this::enableRefactorButtonIfAnySelected);
        refreshLabel.setForeground(JBColor.GRAY);
        scrollPane = ScrollPaneFactory.createScrollPane(treeTable);
        scrollPane.setViewportView(refreshLabel);
        scrollPane.setVisible(true);
        return scrollPane;
    }

    //    /**
    //     * Filters available refactorings suggestions from refactoring list.
    //     *
    //     * @return list of available refactorings suggestions.
    //     */
    //    private List<ExtractMethodCandidateGroup> getAvailableRefactoringSuggestions() {
    //        return treeTableModel.getCandidateRefactoringGroups().stream()
    //                .filter(extractMethodCandidateGroup -> extractMethodCandidateGroup.getCandidates()
    //                        .stream()
    //                        .allMatch(ASTSlice::areSliceStatementsValid))
    //                .collect(toList());
    //    }

    /**
     * Creates button panel and adds action listeners for buttons.
     *
     * @return panel with buttons.
     */
    private JComponent createButtonPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        final JPanel buttonPanel = new JBPanel<JBPanel<JBPanel>>();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        doRefactorButton.setText(KotlinCodeSmellDetectorBundle.message("refactor.button"));
       // doRefactorButton.addActionListener(e -> refactorSelected());
        doRefactorButton.setEnabled(false);
        buttonPanel.add(doRefactorButton);

        refreshButton.setText(KotlinCodeSmellDetectorBundle.message("refresh.button"));
        refreshButton.addActionListener(l -> refreshPanel());
        refreshButton.setEnabled(true);
        buttonPanel.add(refreshButton);

        exportButton.setText(KotlinCodeSmellDetectorBundle.message("export"));
        //exportButton.addActionListener(e -> ExportResultsUtil.export(getAvailableRefactoringSuggestions(), panel));
        exportButton.setEnabled(false);
        buttonPanel.add(exportButton);

        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

//    /**
//     * Preforms the selected refactoring.
//     */
//    private void refactorSelected() {
//        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
//        if (selectedPath != null) {
//            Object o = selectedPath.getLastPathComponent();
//            if (o instanceof ASTSlice) {
//                TransactionGuard.getInstance().submitTransactionAndWait(doExtract((ASTSlice) o));
//            }
//        }
//    }

    /**
     * Enables Refactor button only if any suggestion is selected.
     */
    private void enableRefactorButtonIfAnySelected() {
        boolean isAnySuggestionSelected = false;
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null) {
            Object o = selectedPath.getLastPathComponent();
            if (o instanceof ASTSlice) {
                ASTSlice slice = (ASTSlice) o;
                if (slice.areSliceStatementsValid()) {
                    isAnySuggestionSelected = true;
                }
            }
        }
        doRefactorButton.setEnabled(isAnySuggestionSelected);
    }

    /**
     * Refreshes the panel with suggestions.
     */
    private void refreshPanel() {
        Editor editor = FileEditorManager.getInstance(scope.getProject()).getSelectedTextEditor();
        if (editor != null) {
            editor.getMarkupModel().removeAllHighlighters();
        }
        doRefactorButton.setEnabled(false);
        exportButton.setEnabled(false);
        refreshButton.setEnabled(false);
        scrollPane.setVisible(false);
        calculateRefactorings();
    }

    /**
     * Calculates suggestions for whole project.
     */
    private void calculateRefactorings() {
        Project project = scope.getProject();
        ProjectInfo projectInfo = new ProjectInfo(project);

        final Task.Backgroundable backgroundable = new Task.Backgroundable(project,
                                                                           KotlinCodeSmellDetectorBundle.message("long.method.detect.indicator.status"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    Set<ASTSliceGroup> candidates = getExtractMethodRefactoringOpportunities(projectInfo, indicator);
                    final List<ExtractMethodCandidateGroup> extractMethodCandidateGroups = candidates.stream().filter(Objects::nonNull)
                            .map(sliceGroup ->
                                         sliceGroup.getCandidates().stream()
                                                // .filter(c -> canBeExtracted(c))
                                                 .collect(toSet()))
                            .filter(set -> !set.isEmpty())
                            .map(ExtractMethodCandidateGroup::new)
                            .collect(toList());
                    treeTableModel.setCandidateRefactoringGroups(extractMethodCandidateGroups);
                    ApplicationManager.getApplication().invokeLater(() -> showRefactoringsTable());
                    //  IntelliJDeodorantCounterCollector.getInstance().refactoringFound(project, "extract.method", extractMethodCandidateGroups.size());
                });
            }

            @Override
            public void onCancel() {
                showEmptyPanel();
            }
        };
        runAfterCompilationCheck(backgroundable, scope.getProject(), projectInfo);
    }

    private void showEmptyPanel() {
        scrollPane.setVisible(true);
        scrollPane.setViewportView(refreshLabel);
        refreshButton.setEnabled(true);
    }

    /**
     * Shows treeTable with available refactorings.
     */
    private void showRefactoringsTable() {
        treeTableModel.reload();
        treeTable.setRootVisible(false);
        scrollPane.setViewportView(treeTable);
        scrollPane.setVisible(true);
        exportButton.setEnabled(!treeTableModel.getCandidateRefactoringGroups().isEmpty());
        refreshButton.setEnabled(true);
    }

//    /**
//     * Opens the definition of appropriate method for the selected suggestion by double-clicking or Enter key pressing.
//     */
//    private void openMethodDefinition(InputEvent e) {
//        TreeTableTree treeTableTree = treeTable.getTree();
//        TreePath selectedPath = treeTableTree.getSelectionModel().getSelectionPath();
//        if (selectedPath != null) {
//            Object o = selectedPath.getLastPathComponent();
//            if (o instanceof ASTSlice) {
//                openDefinition(((ASTSlice) o).getSourceMethodDeclaration(), scope, (ASTSlice) o);
//            } else if (o instanceof ExtractMethodCandidateGroup) {
//                expandOrCollapsePath(e, treeTableTree, selectedPath);
//            }
//        }
//    }

//    /**
//     * Checks that the slice can be extracted into a separate method without compilation errors.
//     */
//    private boolean canBeExtracted(ASTSlice slice) {
//        SmartList<FirElement> statementsToExtract = getStatementsToExtract(slice);
//
//        MyExtractMethodProcessor processor = new MyExtractMethodProcessor(scope.getProject(),
//                                                                          null, statementsToExtract.toArray(new PsiElement[0]), slice.getLocalVariableCriterion().getType(),
//                                                                          KotlinCodeSmellDetectorBundle.message("extract.method.refactoring.name"), "", HelpID.EXTRACT_METHOD,
//                                                                          slice.getSourceTypeDeclaration(), slice.getLocalVariableCriterion());
//
//        processor.setOutputVariable();
//
//        try {
//            processor.setShowErrorDialogs(false);
//            return processor.prepare();
//
//        } catch (PrepareFailedException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    /**
     * Collects statements that can be extracted into a separate method.
     */
    public SmartList<FirElement> getStatementsToExtract(ASTSlice slice) {
        List<CFGNode<? extends FirElement>> nodes = slice.getSliceNodes();
        SmartList<FirElement> statementsToExtract = new SmartList<>();

        for (CFGNode<? extends FirElement> cfgNode : nodes) {
            boolean isNotChild = true;
            for (CFGNode<? extends FirElement> node : nodes) {
                if (isChild(node.getFir(), cfgNode.getFir())) {
                    isNotChild = false;
                }
            }
            if (isNotChild) {
                statementsToExtract.add(cfgNode.getFir());
            }
        }
        return statementsToExtract;
    }

//    /**
//     * Extracts statements into new method.
//     *
//     * @param slice computation slice.
//     * @return callback to run when "Refactor" button is selected.
//     */
//    private Runnable doExtract(ASTSlice slice) {
//        return () -> {
//            Editor editor = FileEditorManager.getInstance(slice.getSourceMethodDeclaration().getProject()).getSelectedTextEditor();
//            SmartList<FirElement> statementsToExtract = getStatementsToExtract(slice);
//
//            MyExtractMethodProcessor processor = new MyExtractMethodProcessor(slice.getSourceMethodDeclaration().getProject(),
//                                                                              editor, statementsToExtract.toArray(new PsiElement[0]), slice.getLocalVariableCriterion().getType(),
//                                                                              "", "", HelpID.EXTRACT_METHOD,
//                                                                              slice.getSourceTypeDeclaration(), slice.getLocalVariableCriterion());
//
//            processor.setOutputVariable();
//
//            try {
//                processor.setShowErrorDialogs(true);
//                if (processor.prepare()) {
//                    ExtractMethodHandler.invokeOnElements(slice.getSourceMethodDeclaration().getProject(), processor,
//                                                          slice.getSourceMethodDeclaration().getContainingFile(), true);
//                    // TODO is it needed
//                    if (editor != null) {
//                        //IntelliJDeodorantCounterCollector.getInstance().extractMethodRefactoringApplied(editor.getProject(),
//                        //                                    statementsToExtract.size());
//                    }
//                }
//            } catch (PrepareFailedException e) {
//                e.printStackTrace();
//            }
//        };
//    }
}
