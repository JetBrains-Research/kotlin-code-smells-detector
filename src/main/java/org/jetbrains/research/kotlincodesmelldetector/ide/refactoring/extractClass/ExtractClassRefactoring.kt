package org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty

class ExtractClassRefactoring(
    val sourceFile: KtFile,
    val sourceClass: KtClassOrObject,
    val extractedFieldFragments: Set<KtProperty>,
    val extractedMethods: Set<KtFunction>,
    val delegateMethods: Set<KtFunction>,
    val defaultExtractedTypeName: String
) {
    val project: Project = sourceFile.project
    var extractedClassName: String = defaultExtractedTypeName

    fun apply() {
    }
}