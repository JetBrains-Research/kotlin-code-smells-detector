package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellFacade;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.MoveMethodCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.moveMethod.MoveMethodRefactoring;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static org.jetbrains.research.kotlincodesmelldetector.ide.ui.MoveMethodTableModel.SELECTION_COLUMN_INDEX;

/**
 * Panel for Move Method refactoring.
 */
class MoveMethodPanel extends JPanel {
    @NotNull
    private final AnalysisScope scope;
    @NotNull
    private final MoveMethodTableModel model;
    private final JBTable table = new JBTable();
    private final JButton refreshButton = new JButton();
    private final List<MoveMethodRefactoring> refactorings = new ArrayList<>();
    private JScrollPane scrollPane = new JBScrollPane();
    private final JLabel refreshLabel = new JLabel(
            KotlinCodeSmellDetectorBundle.message("press.refresh.to.find.refactoring.opportunities"),
            SwingConstants.CENTER
    );

    MoveMethodPanel(@NotNull AnalysisScope scope) {
        this.scope = scope;
        setLayout(new BorderLayout());
        model = new MoveMethodTableModel(refactorings);
        setupGUI();
    }

    private void setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonsPanel(), BorderLayout.SOUTH);
    }

    private JScrollPane createTablePanel() {
        new TableSpeedSearch(table);
        table.setModel(model);
        model.setupRenderer(table);
        table.getSelectionModel().setSelectionMode(SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        setupTableLayout();
        refreshLabel.setForeground(JBColor.GRAY);
        scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setViewportView(refreshLabel);
        return scrollPane;
    }

    private void setupTableLayout() {
        final TableColumn selectionColumn = table.getTableHeader().getColumnModel().getColumn(SELECTION_COLUMN_INDEX);
        selectionColumn.setMaxWidth(30);
        selectionColumn.setMinWidth(30);

        final TableColumn dependencies = table.getTableHeader().getColumnModel().getColumn(SELECTION_COLUMN_INDEX);
        dependencies.setMaxWidth(30);
        dependencies.setMinWidth(30);
    }

    private JComponent createButtonsPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        final JPanel buttonsPanel = new JBPanel<JBPanel<JBPanel>>();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        refreshButton.setText(KotlinCodeSmellDetectorBundle.message("refresh.button"));
        refreshButton.addActionListener(l -> refreshPanel());
        buttonsPanel.add(refreshButton);
        panel.add(buttonsPanel, BorderLayout.EAST);

        return panel;
    }

    private void enableButtonsOnConditions() {
        refreshButton.setEnabled(true);
    }

    private void disableAllButtons() {
        refreshButton.setEnabled(false);
    }

    private void refreshPanel() {
        refactorings.clear();
        model.clearTable();
        disableAllButtons();
        scrollPane.setVisible(false);
        calculateRefactorings();
    }

    private void calculateRefactorings() {
        Project project = scope.getProject();
        ProjectInfo projectInfo = new ProjectInfo(project);

        final Task.Backgroundable backgroundable = new Task.Backgroundable(project, KotlinCodeSmellDetectorBundle.message("feature.envy.detect.indicator.status"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    List<MoveMethodCandidateRefactoring> candidates = KotlinCodeSmellFacade.getMoveMethodRefactoringOpportunities(projectInfo, indicator);
                    final List<MoveMethodRefactoring> references = candidates.stream().filter(Objects::nonNull)
                            .map(x ->
                                    new MoveMethodRefactoring(x.getSourceMethod(),
                                            x.getTargetClass().getElement(),
                                            x.getIntersectionWithSourceClass(),
                                            x.getIntersectionWithTargetClass()))
                            .collect(Collectors.toList());
                    refactorings.clear();
                    refactorings.addAll(new ArrayList<>(references));
                    model.updateTable(refactorings);
                    scrollPane.setVisible(true);
                    scrollPane.setViewportView(table);
                    enableButtonsOnConditions();
                });
            }

            @Override
            public void onCancel() {
                showEmptyPanel();
            }
        };

        AbstractRefactoringPanel.runAfterCompilationCheck(backgroundable, project, projectInfo);
    }

    private void showEmptyPanel() {
        scrollPane.setVisible(true);
        scrollPane.setViewportView(refreshLabel);
        refreshButton.setEnabled(true);
    }
}

