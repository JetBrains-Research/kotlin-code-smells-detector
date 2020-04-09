package org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class ExtractClassRefactoring(
    val sourceFile: KtFile,
    val sourceClass: KtElement,
    val extractedFieldFragments: Set<KtProperty>,
    val extractedMethods: Set<KtDeclaration>,
    val delegateMethods: Set<KtDeclaration>,
    val defaultExtractedTypeName: String
) {
    val project: Project = sourceFile.project
    var extractedClassName: String = defaultExtractedTypeName

    fun apply() {
    }
}