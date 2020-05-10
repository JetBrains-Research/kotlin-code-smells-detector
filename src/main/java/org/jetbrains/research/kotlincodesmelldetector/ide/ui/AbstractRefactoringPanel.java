package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.EditorHelper;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableTree;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.RefactoringType;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.RefactoringType.AbstractCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.RefactoringType.AbstractRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.listeners.DoubleClickListener;
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.listeners.ElementSelectionListener;
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.listeners.EnterKeyListener;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRefactoringPanel extends JPanel {
    private static final NotificationGroup NOTIFICATION_GROUP =
            new NotificationGroup(KotlinCodeSmellDetectorBundle.message("kotlincodesmelldetector"), NotificationDisplayType.STICKY_BALLOON, true);
    private final String detectIndicatorStatusTextKey;
    @NotNull
    protected final AnalysisScope scope;
    private final AbstractTreeTableModel model;
    private final TreeTable treeTable;
    private final ActionButton doRefactorButton;
    private final ActionButton refreshButton;
    private final ActionButton exportButton;
    private final ScopeChooserComboBox scopeChooserComboBox;
    private JScrollPane scrollPane = new JBScrollPane();
    private final JLabel refreshLabel = new JLabel(
            KotlinCodeSmellDetectorBundle.message("press.refresh.to.find.refactoring.opportunities"),
            SwingConstants.CENTER
    );
    private final RefactoringType refactoringType;
    private static Notification errorNotification;
    private final int refactorDepth;

    public AbstractRefactoringPanel(@NotNull AnalysisScope scope,
                                    String detectIndicatorStatusTextKey,
                                    RefactoringType refactoringType,
                                    AbstractTreeTableModel model,
                                    int refactorDepth) {
        this.scope = scope;
        this.detectIndicatorStatusTextKey = detectIndicatorStatusTextKey;
        this.refactoringType = refactoringType;
        this.model = model;
        this.treeTable = new TreeTable(model);
        this.refactorDepth = refactorDepth;
        this.doRefactorButton = new ActionButton(refactorAction(), new Presentation(KotlinCodeSmellDetectorBundle.message("refactor.button")), BorderLayout.EAST, new Dimension(26, 24));
        this.refreshButton = new ActionButton(refreshAction(), new Presentation(KotlinCodeSmellDetectorBundle.message("refresh.button")), BorderLayout.EAST, new Dimension(26, 24));
        this.exportButton = new ActionButton(exportAction(), new Presentation(KotlinCodeSmellDetectorBundle.message("export")), BorderLayout.EAST, new Dimension(26, 24));
        this.scopeChooserComboBox = new ScopeChooserComboBox(scope);
        refreshLabel.setForeground(JBColor.GRAY);
        setLayout(new BorderLayout());
        setupGUI();
    }

    public static void runAfterCompilationCheck(Task.Backgroundable afterCompilationBackgroundable,
                                                Project project, ProjectInfo projectInfo) {
        final Task.Backgroundable compilationBackgroundable = new Task.Backgroundable(project, KotlinCodeSmellDetectorBundle.message("project.compiling.indicator.text"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                runAfterCompilationCheck(projectInfo, afterCompilationBackgroundable);
            }
        };

        ProgressManager.getInstance().run(compilationBackgroundable);
    }

    /**
     * Compiles the project and runs the task only if there are no compilation errors.
     */
    private static void runAfterCompilationCheck(ProjectInfo projectInfo, Task task) {
        ApplicationManager.getApplication().invokeLater(() -> {
            for (SmartPsiElementPointer<KtFile> filePointer : projectInfo.getKtFiles()) {
                KtFile file = filePointer.getElement();
                if (file != null && PsiTreeUtil.hasErrorElements(file)) {
                    task.onCancel();
                    AbstractRefactoringPanel.showCompilationErrorNotification(projectInfo.getProject());
                    return;
                }
            }

            ProgressManager.getInstance().run(task);
        });
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createToolbar(), BorderLayout.NORTH);
        registerPsiModificationListener();
        showRefreshingProposal();
    }

    private void removeSelection() {
        treeTable.getTree().setSelectionPath(null);
    }

    /**
     * Shows treeTable with available refactorings.
     */
    private void showRefactoringsTable() {
        removeSelection();
        scrollPane.setVisible(true);
        model.reload();
        scrollPane.setViewportView(treeTable);
    }

    /**
     * Hides treeTable with refactorings and shows text which proposes refreshing available refactorings.
     */
    protected void showRefreshingProposal() {
        removeSelection();
        if (errorNotification != null && !errorNotification.isExpired()) {
            errorNotification.expire();
        }
        scrollPane.setVisible(true);
        scrollPane.setViewportView(refreshLabel);
    }

    /**
     * Hides treeTable with refactorings and leaves panel empty
     */
    private void showEmptyPanel() {
        removeSelection();
        scrollPane.setVisible(false);
    }

    /**
     * Creates scrollable treeTable panel and adds listeners.
     *
     * @return treeTable which can display available refactorings.
     */
    private JScrollPane createTablePanel() {
        treeTable.setRootVisible(false);
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.addMouseListener((DoubleClickListener) this::highlightCode);
        treeTable.addKeyListener((EnterKeyListener) this::highlightCode);
        treeTable.getTree().addTreeSelectionListener((ElementSelectionListener) this::enableRefactorButtonIfAnySelected);
        scrollPane = ScrollPaneFactory.createScrollPane(treeTable);
        return scrollPane;
    }

    /**
     * Adds a listener that invalidates found refactoring opportunities if the structure of PSI is changed.
     */
    private void registerPsiModificationListener() {
        MessageBus projectMessageBus = scope.getProject().getMessageBus();
        projectMessageBus.connect().subscribe(PsiModificationTracker.TOPIC, () -> ApplicationManager.getApplication().invokeLater(this::showRefreshingProposal));
    }

    /**
     * Creates button panel and adds action listeners for buttons.
     *
     * @return panel with buttons.
     */
    private JPanel createToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(doRefactorButton.getAction());
        actionGroup.add(refreshButton.getAction());
        actionGroup.add(exportButton.getAction());
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.MAIN_TOOLBAR, actionGroup, true);
        JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.add(scopeChooserComboBox);
        buttonsPanel.add(toolbar.getComponent());
        return buttonsPanel;
    }

    /**
     * Preforms selected refactoring.
     */
    private void refactorSelected() {
        TreePath selectedPath = treeTable.getTree().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == refactorDepth) {
            AbstractCandidateRefactoring computationSlice = (AbstractCandidateRefactoring) selectedPath.getLastPathComponent();
            removeSelection();
            doRefactor(computationSlice);
        }
    }

    protected abstract void doRefactor(AbstractCandidateRefactoring candidateRefactoring);

    /**
     * Enables Refactor button only if a suggestion is selected.
     */
    private void enableRefactorButtonIfAnySelected() {
        boolean isAnySuggestionSelected = false;
        TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();
        if (selectedPath != null && selectedPath.getPathCount() == refactorDepth) {
            Object o = selectedPath.getLastPathComponent();
            if (refactoringType.instanceOfCandidateRefactoring(o)) {
                isAnySuggestionSelected = true;
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
        showEmptyPanel();
        calculateRefactorings();
    }

    /**
     * Calculates suggestions for whole project.
     */
    private void calculateRefactorings() {
        ProjectInfo projectInfo = new ProjectInfo(scope);

        final Task.Backgroundable backgroundable = new Task.Backgroundable(scope.getProject(),
                KotlinCodeSmellDetectorBundle.message(detectIndicatorStatusTextKey), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    List<RefactoringType.AbstractCandidateRefactoringGroup> candidates =
                            refactoringType.getRefactoringOpportunities(projectInfo, indicator);
                    if (candidates == null) {
                        showCompilationErrorNotification(getProject());
                        candidates = new ArrayList<>();
                    }
                    model.setCandidateRefactoringGroups(candidates);
                    ApplicationManager.getApplication().invokeLater(() -> showRefactoringsTable());
                });
            }

            @Override
            public void onCancel() {
                showRefreshingProposal();
            }
        };

        runAfterCompilationCheck(backgroundable, scope.getProject(), projectInfo);
    }

    /**
     * Highlights refactoring-specific code fragment.
     */
    private void highlightCode(InputEvent e) {
        TreeTableTree treeTableTree = treeTable.getTree();
        TreePath selectedPath = treeTableTree.getSelectionModel().getSelectionPath();
        if (selectedPath != null) {
            Object o = selectedPath.getLastPathComponent();
            if (refactoringType.instanceOfCandidateRefactoring(o)) {
                AbstractCandidateRefactoring refactoring = (AbstractCandidateRefactoring) o;
                refactoring.highlightCode();
            } else {
                expandOrCollapsePath(e, treeTableTree, selectedPath);
            }
        }
    }

    /**
     * Collapse if the selected path is extended, otherwise expand.
     */
    public static void expandOrCollapsePath(InputEvent e, TreeTableTree treeTableTree, TreePath selectedPath) {
        if (e instanceof KeyEvent) {
            if (treeTableTree.isExpanded(selectedPath)) {
                treeTableTree.collapsePath(selectedPath);
            } else {
                treeTableTree.expandPath(selectedPath);
            }
        }
    }

    public AbstractRefactoring getAbstractRefactoringFromAbstractCandidateRefactoring(AbstractCandidateRefactoring candidate) {
        return refactoringType.newAbstractRefactoring(candidate);
    }

    /**
     * Opens definition of method and highlights specified element in the method.
     */
    public static void highlightStatement(@Nullable KtDeclaration sourceMethod,
                                          AnalysisScope scope,
                                          PsiElement statement,
                                          boolean openInEditor) {
        new Task.Backgroundable(scope.getProject(), "Search Definition") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
            }

            @Override
            public void onSuccess() {
                if (sourceMethod == null || !statement.isValid()) {
                    return;
                }
                highlightPsiElement(statement, openInEditor);
            }
        }.queue();
    }

    public static void highlightMethod(@Nullable KtDeclaration sourceMethod,
                                       AnalysisScope scope, boolean openInEditor) {
        highlightStatement(sourceMethod, scope, sourceMethod, openInEditor);
    }

    public static void highlightProperty(@Nullable KtDeclaration field, AnalysisScope scope, boolean openInEditor) {
        new Task.Backgroundable(scope.getProject(), "Search Definition") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
            }

            @Override
            public void onSuccess() {
                if (field == null || !field.isValid()) {
                    return;
                }

                highlightPsiElement(field, openInEditor);
            }
        }.queue();
    }

    private static void highlightPsiElement(PsiElement psiElement, boolean openInEditor) {
        if (openInEditor) {
            EditorHelper.openInEditor(psiElement);
        }

        Editor editor = FileEditorManager.getInstance(psiElement.getProject()).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        editor.getMarkupModel().addRangeHighlighter(
                psiElement.getTextRange().getStartOffset(),
                psiElement.getTextRange().getEndOffset(),
                HighlighterLayer.SELECTION,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
        );
    }

    public static void removeHighlighters(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        editor.getMarkupModel().removeAllHighlighters();
    }

    public static void showCompilationErrorNotification(Project project) {
        errorNotification = NOTIFICATION_GROUP.createNotification(KotlinCodeSmellDetectorBundle.message("compilation.error.notification.text"), MessageType.ERROR);
        Notifications.Bus.notify(errorNotification, project);
    }
    private AnAction refactorAction() {
        return new AnAction(KotlinCodeSmellDetectorBundle.message("refactor.button"), null, AllIcons.Actions.RefactoringBulb) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) { refactorSelected(); }

            @Override
            public void update(@NotNull AnActionEvent e) { e.getPresentation().setEnabled(false); }
        };
    }
    private AnAction refreshAction() {
        return new AnAction(KotlinCodeSmellDetectorBundle.message("refresh.button"), null, AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) { refreshPanel(); }

            @Override
            public void update(@NotNull AnActionEvent e) { e.getPresentation().setEnabled(scrollPane.isVisible()); }
        };
    }
    private AnAction exportAction() {
        return new AnAction(KotlinCodeSmellDetectorBundle.message("export"), null, AllIcons.ToolbarDecorator.Export) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {}

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(scrollPane.isVisible() && !model.getCandidateRefactoringGroups().isEmpty());
            }
        };
    }
}