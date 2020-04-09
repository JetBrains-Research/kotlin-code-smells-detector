package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.research.kotlincodesmelldetector.utils.extractClasses
import org.jetbrains.research.kotlincodesmelldetector.utils.extractFiles
import org.jetbrains.research.kotlincodesmelldetector.utils.toPointer

class ProjectInfo(val project: Project) {
    val ktFiles: List<SmartPsiElementPointer<KtFile>>
    val classes: List<SmartPsiElementPointer<KtElement>>

    init {
        ktFiles = extractFiles(project).map { file -> file.toPointer() }

        val classes = mutableListOf<SmartPsiElementPointer<KtElement>>()
        ktFiles.forEach { file ->
            classes.addAll(
                extractClasses(file.element).map { clazz -> clazz.toPointer() })
        }
        this.classes = classes
    }
}