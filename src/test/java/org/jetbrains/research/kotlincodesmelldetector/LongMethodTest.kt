package org.jetbrains.research.kotlincodesmelldetector

import com.intellij.analysis.AnalysisScope
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.ASTSliceGroup
import org.jetbrains.research.kotlincodesmelldetector.utils.extractFirSimpleFunctions
import org.jetbrains.research.kotlincodesmelldetector.utils.getCurrentFirFileOpenInEditor

private const val PATH_TO_TESTDATA = "src/test/resources/testdata/"
private const val PATH_TO_TESTS = "/longmethod/"

internal class LongMethodTest : LightJavaCodeInsightFixtureTestCase() {

    private fun getExtractMethodGroups(classFileName: String): List<ASTSliceGroup> {
        val project = getProject(classFileName)
        val projectInfo = ProjectInfo(AnalysisScope(project))
        val set: MutableSet<ASTSliceGroup> =
                KotlinCodeSmellFacade.getExtractMethodRefactoringOpportunities(projectInfo, ProgressIndicatorBase())
        return set.toList()
    }

    private fun getProject(classFileName: String): Project {
        myFixture.testDataPath = PATH_TO_TESTDATA
        myFixture.configureByFile(PATH_TO_TESTS + classFileName)
        return myFixture.project
    }

    fun testSimpleFunction() {
        val classFileName = "TestSimpleFunction.kt"

        val groups = getExtractMethodGroups(classFileName)
        assertTrue(groups.size == 1 && groups[0].candidates.size == 1)
        val candidate = groups[0].candidates.toTypedArray()[0]
        assertTrue(candidate.sliceStatements.size == 3)
        val sliceStatements = candidate.sliceStatements
        TestCase.assertEquals(1, sliceStatements.filterIsInstance<FirProperty>()
                .filter { it.name.asString() == "a" }.size)
        TestCase.assertEquals(2, sliceStatements.filterIsInstance<FirVariableAssignment>()
                .map { it.lValue }.filterIsInstance<FirNamedReference>()
                .filter { it.name.asString() == "a" }.size)
    }

    fun testFunctionsInClasses() {
        val classFileName = "TestFunctionsInClasses.kt"
        val functions = extractFirSimpleFunctions(getCurrentFirFileOpenInEditor(getProject(classFileName)))
        TestCase.assertEquals(5, functions.size)
    }
}