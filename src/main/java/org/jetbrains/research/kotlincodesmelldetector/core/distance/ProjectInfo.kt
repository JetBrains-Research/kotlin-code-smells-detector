package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.analysis.AnalysisScope
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.research.kotlincodesmelldetector.utils.extractClasses
import org.jetbrains.research.kotlincodesmelldetector.utils.extractFiles
import org.jetbrains.research.kotlincodesmelldetector.utils.toPointer

class ProjectInfo(val scope: AnalysisScope) {
    val project = scope.project
    val ktFiles: List<SmartPsiElementPointer<KtFile>>
    val classes: List<SmartPsiElementPointer<KtElement>>

    init {
        ktFiles = extractFiles(project).filter { file -> scope.contains(file) }.map { file -> file.toPointer() }

        this.classes = mutableListOf()
        ktFiles.forEach { file ->
            classes.addAll(
                extractClasses(file.element).map { clazz -> clazz.toPointer() })
        }
    }
}