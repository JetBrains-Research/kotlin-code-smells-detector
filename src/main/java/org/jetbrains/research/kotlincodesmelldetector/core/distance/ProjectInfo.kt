package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.research.kotlincodesmelldetector.utils.PsiUtils
import org.jetbrains.research.kotlincodesmelldetector.utils.toPointer
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

class ProjectInfo(val project: Project) {
    val ktFiles: List<SmartPsiElementPointer<KtFile>>
    val classes: List<SmartPsiElementPointer<KtClassOrObject>>

    init {
        ktFiles = PsiUtils.extractFiles(project).map { file -> file.toPointer() }

        val classes = mutableListOf<SmartPsiElementPointer<KtClassOrObject>>()
        ktFiles.forEach { file -> classes.addAll(PsiUtils.extractClasses(file.element).map{clazz -> clazz.toPointer()}) }
        this.classes = classes
    }
}