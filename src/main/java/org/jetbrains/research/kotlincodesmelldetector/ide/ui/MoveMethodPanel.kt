package org.jetbrains.research.kotlincodesmelldetector.ide.ui

import com.intellij.analysis.AnalysisScope
import com.intellij.analysis.AnalysisUIOptions
import com.intellij.analysis.BaseAnalysisActionDialog
import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.JBPanel
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
internal class MoveMethodPanel(private val project: Project) : JPanel() {
    private val model: MoveMethodTableModel
    private val defaultScope = AnalysisScope(project)
    private var customScope = defaultScope
    private val table = JBTable()
    private val refreshButton = JButton()
    private val scopeButton = JButton(AllIcons.General.ProjectConfigurable)
    private val selectAllButton = JButton(AllIcons.Actions.Selectall)
    private val deselectAllButton = JButton(AllIcons.Actions.Unselectall)
    private val refactorings: MutableList<MoveMethodRefactoring> = ArrayList()
    private var scrollPane: JScrollPane = JBScrollPane()
    private val refreshLabel = JLabel(
            KotlinCodeSmellDetectorBundle.message("press.refresh.to.find.refactoring.opportunities"),
            SwingConstants.CENTER
    )

    private fun setupGUI() {
        add(createTablePanel(), BorderLayout.CENTER)
        add(createButtonsPanel(), BorderLayout.SOUTH)
        add(createToolBar(), BorderLayout.EAST)
    }

    private fun createToolBar(): JToolBar {
        val toolBar = JToolBar(JToolBar.VERTICAL)
        toolBar.isFloatable = false
        scopeButton.preferredSize = Dimension(30, 30)
        selectAllButton.preferredSize = Dimension(30, 30)
        deselectAllButton.preferredSize = Dimension(30, 30)
        scopeButton.toolTipText = KotlinCodeSmellDetectorBundle.message("specify.scope.button")
        selectAllButton.toolTipText = KotlinCodeSmellDetectorBundle.message("select.all.button")
        deselectAllButton.toolTipText = KotlinCodeSmellDetectorBundle.message("deselect.all.button")
        scopeButton.addActionListener { specifyScope() }
        toolBar.add(scopeButton)
        toolBar.add(selectAllButton)
        toolBar.add(deselectAllButton)
        return toolBar

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

    private fun createButtonsPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val buttonsPanel: JPanel = JBPanel<JBPanel<JBPanel<*>>>()
        buttonsPanel.layout = FlowLayout(FlowLayout.RIGHT)
        refreshButton.text = KotlinCodeSmellDetectorBundle.message("refresh.button")
        refreshButton.addActionListener { refreshPanel() }
        buttonsPanel.add(refreshButton)
        panel.add(buttonsPanel, BorderLayout.EAST)
        return panel
    }

    private fun enableButtonsOnConditions() {
        refreshButton.isEnabled = true
    }

    private fun disableAllButtons() {
        refreshButton.isEnabled = false
    }

    private fun refreshPanel() {
        refactorings.clear()
        model.clearTable()
        disableAllButtons()
        scrollPane.isVisible = false
        calculateRefactorings()
    }

    private fun specifyScope() {
        val options = AnalysisUIOptions.getInstance(project)
        val items = BaseAnalysisActionDialog.standardItems(project, defaultScope, null, null)
        val dialog = BaseAnalysisActionDialog("Specify " + KotlinCodeSmellDetectorBundle.message("feature.envy.scope.title"), KotlinCodeSmellDetectorBundle.message("feature.envy.scope.title"),
                project, items, options, true, false)
        ApplicationManager.getApplication().invokeLater {
            dialog.show()
            if (dialog.isOK) {
                customScope = dialog.getScope(defaultScope)
            }
        }
    }

    private fun calculateRefactorings() {
        val projectInfo = ProjectInfo(customScope)
        val backgroundable: Backgroundable = object : Backgroundable(project, KotlinCodeSmellDetectorBundle.message("feature.envy.detect.indicator.status"), true) {
            override fun run(indicator: ProgressIndicator) {
                ApplicationManager.getApplication().runReadAction {
                    val candidates = KotlinCodeSmellFacade.getMoveMethodRefactoringOpportunities(projectInfo, indicator)
                    val references = candidates.stream().filter { obj: MoveMethodCandidateRefactoring? -> Objects.nonNull(obj) }
                            .map { candidate: MoveMethodCandidateRefactoring ->
                                MoveMethodRefactoring(candidate.sourceMethod,
                                        candidate.targetClass.element,
                                        candidate.distinctSourceDependencies,
                                        candidate.distinctTargetDependencies)
                            }
                            .collect(Collectors.toList())
                    refactorings.clear()
                    refactorings.addAll(ArrayList(references))
                    model.updateTable(refactorings)
                    scrollPane.isVisible = true
                    scrollPane.setViewportView(table)
                    enableButtonsOnConditions()
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
        setupGUI()
    }
}