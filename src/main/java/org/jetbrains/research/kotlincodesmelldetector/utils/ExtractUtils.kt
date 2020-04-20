package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import org.apache.commons.lang.ArrayUtils
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.util.ArrayList
import java.util.Optional

private const val FILE_TYPE_NAME = "Kotlin"

fun extractFiles(project: Project?): List<KtFile> {
    val ktFiles: MutableList<KtFile> = ArrayList()
    ProjectFileIndex.SERVICE.getInstance(project).iterateContent { file: VirtualFile? ->
        val psiFile = PsiManager.getInstance(project!!).findFile(file!!)
        if (psiFile != null && !psiFile.isDirectory
            && FILE_TYPE_NAME == psiFile.fileType.name
        ) {
            ktFiles.add(psiFile as KtFile)
        }
        true
    }
    return ktFiles
}

fun getCurrentFileOpenInEditor(project: Project?): KtFile? {
    val currentEditor = FileEditorManager.getInstance(project!!).selectedEditor
    if (currentEditor != null) {
        val currentFile = currentEditor.file
        if (currentFile != null) {
            val psiFile = PsiManager.getInstance(project).findFile(currentFile)
            if (psiFile is KtFile) {
                return psiFile
            }
        }
    }
    return null
}

fun extractClasses(ktElement: KtElement?): List<KtElement> {
    val declarations =
        when (ktElement) {
            is KtFile -> {
                ktElement.declarations
            }
            is KtClassOrObject -> {
                ktElement.declarations
            }
            else -> {
                return ArrayList()
            }
        }

    val classes = declarations.filterIsInstance<KtClassOrObject>()

    val result: MutableList<KtElement> = ArrayList(classes)
    for (ktClassOrObject in classes) {
        for (ktDeclaration in ktClassOrObject.declarations) {
            if (ktDeclaration is KtClassOrObject) {
                result.add(ktDeclaration)
                result.addAll(extractClasses(ktDeclaration))
            }
        }
    }
    if (ktElement is KtFile) {
        result.add(ktElement)
    }
    return result
}

fun isChild(parent: KtElement, child: KtElement): Boolean {
    return if (parent == child) false else child.textRange.startOffset >= parent.textRange.startOffset
        && child.textRange.endOffset <= parent.textRange.endOffset
}

fun isTestClass(ktClass: KtClassOrObject): Boolean {
    val file = ktClass.containingFile as KtFile
    return isInsideTestDirectory(file)
}

private fun isInsideTestDirectory(file: KtFile): Boolean {
    var directory: PsiDirectory? = getDirectoryWithRootPackageFor(file)
    while (directory != null) {
        val dirName = directory.name.toLowerCase()
        if (dirName == "test" || dirName == "tests") {
            return true
        }
        directory = directory.parentDirectory
    }
    return false
}

private fun getDirectoryWithRootPackageFor(file: KtFile): PsiDirectory? {
    val packageName = file.packageFqName.asString()
    val packageSequence = packageName.split(".")
    var directory = file.containingDirectory ?: throw IllegalStateException("File has no parent directory")
    for (packagePart in packageSequence.reversed()) {
        if (packagePart != directory.name) {
            return null
        }
        directory = directory.parentDirectory ?: return null
    }

    return directory
}