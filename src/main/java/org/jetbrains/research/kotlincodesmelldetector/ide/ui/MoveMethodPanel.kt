package org.jetbrains.research.kotlincodesmelldetector.ide.ui

import com.intellij.analysis.AnalysisScope
import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellFacade
import org.jetbrains.research.kotlincodesmelldetector.core.distance.MoveMethodCandidateRefactoring
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.moveMethod.MoveMethodRefactoring
import org.jetbrains.research.kotlincodesmelldetector.ide.ui.listeners.DoubleClickListener
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.InputEvent
import java.util.*
import java.util.stream.Collectors
import javax.swing.*

/**
 * Panel for Move Method refactoring.
 */
internal class MoveMethodPanel(private val project: Project) : SimpleToolWindowPanel(false) {
    private val model: MoveMethodTableModel
    private val table = JBTable()
    private val refactorButton: ActionButton
    private val refreshButton: ActionButton
    private val exportButton: ActionButton
    private val selectButton: ActionButton
    private val deselectButton: ActionButton
    private val scopeChooserComboBox: ScopeChooserComboBox

    private val refactorings: MutableList<MoveMethodRefactoring> = ArrayList()
    private var scrollPane: JScrollPane = JBScrollPane()
    private val refreshLabel = JLabel(
        KotlinCodeSmellDetectorBundle.message("press.refresh.to.find.refactoring.opportunities"),
        SwingConstants.CENTER
    )
    private val defaultScope = AnalysisScope(project)
    private var enableButtons = true

    private fun setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER)
        add(createToolbar(), BorderLayout.NORTH)
    }

    private fun createToolbar(): JPanel {
        val actionGroup = DefaultActionGroup()
        actionGroup.add(refactorButton.action)
        actionGroup.add(refreshButton.action)
        actionGroup.add(exportButton.action)
        actionGroup.addSeparator()
        actionGroup.add(selectButton.action)
        actionGroup.add(deselectButton.action)
        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.MAIN_TOOLBAR, actionGroup, true)
        val buttonsPanel = JPanel(BorderLayout())
        buttonsPanel.layout = FlowLayout(FlowLayout.LEFT)
        buttonsPanel.add(scopeChooserComboBox)
        buttonsPanel.add(toolbar.component)
        return buttonsPanel
    }

    private fun createTablePanel(): JScrollPane {
        TableSpeedSearch(table)
        table.model = model
        model.setupRenderer(table)
        table.addMouseListener(DoubleClickListener { e: InputEvent -> onDoubleClick(e) })
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.autoCreateRowSorter = true
        setupTableLayout()
        refreshLabel.foreground = JBColor.GRAY
        scrollPane = ScrollPaneFactory.createScrollPane(table)
        scrollPane.setViewportView(refreshLabel)
        return scrollPane
    }

    private fun setupTableLayout() {
        val selectionColumn = table.tableHeader.columnModel.getColumn(MoveMethodTableModel.SELECTION_COLUMN_INDEX)
        selectionColumn.maxWidth = 30
        selectionColumn.minWidth = 30
        val dependencies = table.tableHeader.columnModel.getColumn(MoveMethodTableModel.SELECTION_COLUMN_INDEX)
        dependencies.maxWidth = 30
        dependencies.minWidth = 30
    }

    private fun refreshPanel() {
        refactorings.clear()
        model.clearTable()
        enableButtons = false
        scrollPane.isVisible = false
        calculateRefactorings()
    }

    private fun calculateRefactorings() {
        val projectInfo = ProjectInfo(scopeChooserComboBox.getScope())
        val backgroundable: Backgroundable = object : Backgroundable(
            project,
            KotlinCodeSmellDetectorBundle.message("feature.envy.detect.indicator.status"),
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                ApplicationManager.getApplication().runReadAction {
                    val candidates = KotlinCodeSmellFacade.getMoveMethodRefactoringOpportunities(projectInfo, indicator)
                    val references =
                        candidates.stream().filter { obj: MoveMethodCandidateRefactoring? -> Objects.nonNull(obj) }
                            .map { candidate: MoveMethodCandidateRefactoring ->
                                MoveMethodRefactoring(
                                    candidate.sourceMethod,
                                    candidate.targetClass.element,
                                    candidate.distinctSourceDependencies,
                                    candidate.distinctTargetDependencies
                                )
                            }
                            .collect(Collectors.toList())
                    refactorings.clear()
                    refactorings.addAll(ArrayList(references))
                    model.updateTable(refactorings)
                    scrollPane.isVisible = true
                    scrollPane.setViewportView(table)
                    enableButtons = true
                }
            }

            override fun onCancel() {
                showEmptyPanel()
            }
        }
        AbstractRefactoringPanel.runAfterCompilationCheck(backgroundable, project, projectInfo)
    }

    private fun showEmptyPanel() {
        scrollPane.isVisible = true
        scrollPane.setViewportView(refreshLabel)
        refreshButton.isEnabled = true
    }

    private fun onDoubleClick(e: InputEvent) {
        val selectedRow = if (table.selectedRow == -1) -1 else table.convertRowIndexToModel(table.selectedRow)
        val selectedColumn = table.selectedColumn
        if (selectedRow == -1 || selectedColumn == -1 || selectedColumn == MoveMethodTableModel.SELECTION_COLUMN_INDEX) {
            return
        }
        openDefinition(model.getUnitAt(selectedRow, selectedColumn).orElse(null), defaultScope)
    }

    companion object {
        private fun openDefinition(unit: KtElement?, scope: AnalysisScope) {
            object : Backgroundable(scope.project, "Search Definition") {
                private var result: PsiElement? = null
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    result = unit
                }

                override fun onSuccess() {
                    if (result != null) {
                        EditorHelper.openInEditor(result!!)
                    }
                }
            }.queue()
        }
    }

    init {
        layout = BorderLayout()
        model = MoveMethodTableModel(refactorings)
        scopeChooserComboBox = ScopeChooserComboBox(defaultScope)
        refactorButton = ActionButton(refactorAction(), Presentation(KotlinCodeSmellDetectorBundle.message("refactor.button")), BorderLayout.EAST, Dimension(26, 24))
        refreshButton = ActionButton(refreshAction(), Presentation(KotlinCodeSmellDetectorBundle.message("refresh.button")), BorderLayout.EAST, Dimension(26, 24))
        exportButton = ActionButton(exportAction(), Presentation(KotlinCodeSmellDetectorBundle.message("export")), BorderLayout.EAST, Dimension(26, 24))
        selectButton = ActionButton(selectAllAction(), Presentation(KotlinCodeSmellDetectorBundle.message("select.all.button")), BorderLayout.EAST, Dimension(26, 24))
        deselectButton = ActionButton(deselectAllAction(), Presentation(KotlinCodeSmellDetectorBundle.message("deselect.all.button")), BorderLayout.EAST, Dimension(26, 24))
        setupGUI()
    }

    private fun refactorAction(): AnAction = object : AnAction(KotlinCodeSmellDetectorBundle.message("refactor.button"), null, AllIcons.Actions.RefactoringBulb) {
        // TODO("Not yet implemented")
        override fun actionPerformed(e: AnActionEvent) {}
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = false
        }
    }

    private fun refreshAction(): AnAction = object : AnAction(KotlinCodeSmellDetectorBundle.message("refresh.button"), null, AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            refreshPanel()
        }
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = enableButtons
        }
    }

    private fun exportAction(): AnAction = object : AnAction(KotlinCodeSmellDetectorBundle.message("export"), null, AllIcons.ToolbarDecorator.Export) {
        // TODO("Not yet implemented")
        override fun actionPerformed(e: AnActionEvent) {}
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = false
        }
    }

    private fun selectAllAction(): AnAction = object : AnAction(KotlinCodeSmellDetectorBundle.message("select.all.button"), null, AllIcons.Actions.Selectall) {
        // TODO("Not yet implemented")
        override fun actionPerformed(e: AnActionEvent) {}
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = false
        }
    }

    private fun deselectAllAction(): AnAction = object : AnAction(KotlinCodeSmellDetectorBundle.message("deselect.all.button"), null, AllIcons.Actions.Unselectall) {
        // TODO("Not yet implemented")
        override fun actionPerformed(e: AnActionEvent) {}
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = false
        }
    }
}