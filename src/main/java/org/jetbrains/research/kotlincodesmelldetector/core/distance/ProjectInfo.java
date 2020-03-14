package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import com.intellij.openapi.project.Project;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlincodesmelldetector.utils.PsiUtils;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectInfo {
    private final List<SmartPsiElementPointer<KtFile>> ktFiles;
    private final List<SmartPsiElementPointer<KtClassOrObject>> ktClasses;
    private final Project project;

    public ProjectInfo(Project project) {
        this.project = project;
        this.ktFiles = PsiUtils.extractFiles(project);

        this.ktClasses = ktFiles.stream()
                .flatMap(ktFilePointer -> PsiUtils.extractClasses(ktFilePointer.getElement()).stream())
                .collect(Collectors.toList());
    }

    public List<SmartPsiElementPointer<KtClassOrObject>> getClasses() {
        return ktClasses;
    }

    public List<SmartPsiElementPointer<KtFile>> getKtFiles() {
        return ktFiles;
    }

    public Project getProject() {
        return project;
    }
}
