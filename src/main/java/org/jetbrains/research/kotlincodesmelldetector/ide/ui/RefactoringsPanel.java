package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;
import org.jetbrains.research.kotlincodesmelldetector.utils.KtUtilsKt;

import javax.swing.*;

class RefactoringsPanel extends SimpleToolWindowPanel {

    RefactoringsPanel(Project project) {
        super(false, true);
        addRefactoringPanels(project);
    }

    /**
     * Adds a panel for each code smell to the main panel.
     *
     * @param project current project.
     */
    private void addRefactoringPanels(Project project) {
        JTabbedPane jTabbedPane = new JBTabbedPane();
        jTabbedPane.add(KotlinCodeSmellDetectorBundle.message("god.class.smell.name"), new GodClassPanel(project));
        jTabbedPane.add(KotlinCodeSmellDetectorBundle.message("feature.envy.smell.name"), new MoveMethodPanel(project));
        // TODO remove scope?
        jTabbedPane.add(KotlinCodeSmellDetectorBundle.message("long.method.smell.name"), new ExtractMethodPanel(new AnalysisScope(project)));
        setContent(jTabbedPane);
    }
}
