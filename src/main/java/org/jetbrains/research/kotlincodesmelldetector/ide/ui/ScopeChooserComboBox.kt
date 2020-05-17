package org.jetbrains.research.kotlincodesmelldetector.ide.ui

import com.intellij.analysis.AnalysisScope
import com.intellij.ide.util.scopeChooser.EditScopesDialog
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.ui.ComboboxWithBrowseButton
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import javax.swing.DefaultComboBoxModel


class ScopeChooserComboBox(private val projectScope: AnalysisScope) : ComboboxWithBrowseButton() {
    private var customScope: AnalysisScope? = null
    private val project = projectScope.project

    init {
        setTextFieldPreferredWidth(40)
        rebuildModelAndSelectScope(null)
        addActionListener { browseCustomScope() }
    }

    private fun rebuildModelAndSelectScope(scope: String?) {
        val comboBoxModel = DefaultComboBoxModel<String>()
        comboBoxModel.addElement(KotlinCodeSmellDetectorBundle.message("project.files"))
        comboBoxModel.addElement(KotlinCodeSmellDetectorBundle.message("current.file"))
        comboBoxModel.addElement(KotlinCodeSmellDetectorBundle.message("open.files"))
        scope?.let { comboBoxModel.addElement(scope) }
        comboBox.setModel(comboBoxModel)
        comboBox.selectedItem = scope ?: KotlinCodeSmellDetectorBundle.message("project.files")
    }

    private fun browseCustomScope() {
        val dialog = EditScopesDialog.showDialog(project, null)
        if (dialog.isOK) {
            dialog.selectedScope?.let { selectedScope ->
                customScope = AnalysisScope(GlobalSearchScopesCore.filterScope(project, selectedScope), project)
                rebuildModelAndSelectScope(selectedScope.name)
            }

        }
    }

    fun getScope(): AnalysisScope {
        return when (comboBox.selectedItem) {
            KotlinCodeSmellDetectorBundle.message("project.files") -> projectScope
            KotlinCodeSmellDetectorBundle.message("current.file") -> getCurrentFileScope()
            KotlinCodeSmellDetectorBundle.message("open.files") -> getOpenFilesScope()
            else -> customScope
        } ?: projectScope
    }

    private fun getOpenFilesScope() = AnalysisScope(project, FileEditorManager.getInstance(project).openFiles.toList())

    private fun getCurrentFileScope(): AnalysisScope? {
        val fileEditor = FileEditorManager.getInstance(project).selectedEditor
        fileEditor?.file?.let { file ->
            PsiManager.getInstance(project).findFile(file)?.let { psiFile ->
                return AnalysisScope(psiFile)
            }
        }
        return null
    }
}