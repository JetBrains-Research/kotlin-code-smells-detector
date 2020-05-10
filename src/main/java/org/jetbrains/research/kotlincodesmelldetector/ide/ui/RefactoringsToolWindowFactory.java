package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;

class RefactoringsToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        Content moveMethodPanel = contentManager.getFactory().createContent(new MoveMethodPanel(project), KotlinCodeSmellDetectorBundle.message("feature.envy.smell.name"), false);
        Content godClassPanel = contentManager.getFactory().createContent(new GodClassPanel(new AnalysisScope(project)), KotlinCodeSmellDetectorBundle.message("god.class.smell.name"), false);
        contentManager.addContent(moveMethodPanel);
        contentManager.addContent(godClassPanel);
    }

}
