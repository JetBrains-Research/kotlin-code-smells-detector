package org.jetbrains.research.kotlincodesmelldetector

import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.research.kotlincodesmelldetector.core.FeatureEnvyVisualizationData
import org.jetbrains.research.kotlincodesmelldetector.core.distance.MoveMethodCandidateRefactoring
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo

private const val PATH_TO_TESTDATA = "src/test/resources/testdata/"
private const val PATH_TO_TESTS = "/core/distance/featureenvy/"

internal class FeatureEnvyTest : LightJavaCodeInsightFixtureTestCase() {

    private fun getMoveMethodCandidates(fileName: String): List<MoveMethodCandidateRefactoring> {
        myFixture.testDataPath = PATH_TO_TESTDATA
        myFixture.configureByFile(PATH_TO_TESTS + fileName)
        myFixture.allowTreeAccessForAllFiles()
        val project = myFixture.project
        val projectInfo = ProjectInfo(project)
        return KotlinCodeSmellFacade.getMoveMethodRefactoringOpportunities(projectInfo, ProgressIndicatorBase())
    }

    fun testFieldAccessesCount() {
        val fileName = "TestFieldAccessesCount.kt"
        val refactorings: List<MoveMethodCandidateRefactoring> = getMoveMethodCandidates(fileName)
        UsefulTestCase.assertEquals(1, refactorings.size)
        val featureEnvyVisualizationData: FeatureEnvyVisualizationData = refactorings[0].visualizationData
        assertEquals("testMethod", featureEnvyVisualizationData.methodToBeMoved.name)
        assertEquals(3, featureEnvyVisualizationData.distinctTargetDependencies)
        assertEquals(1, featureEnvyVisualizationData.distinctSourceDependencies)
    }

    fun testTargetMethodInvocationsCount() {
        val fileName = "TestTargetMethodInvocationsCount.kt"
        val refactorings: List<MoveMethodCandidateRefactoring> = getMoveMethodCandidates(fileName)
        UsefulTestCase.assertEquals(1, refactorings.size)
        val featureEnvyVisualizationData: FeatureEnvyVisualizationData = refactorings[0].visualizationData
        assertEquals("testMethod", featureEnvyVisualizationData.methodToBeMoved.name)
        assertEquals(3, featureEnvyVisualizationData.distinctTargetDependencies)
        assertEquals(0, featureEnvyVisualizationData.distinctSourceDependencies)
    }

    fun testFieldAccessThroughParameterCount() {
        val fileName = "TestFieldAccessThroughParameterCount.kt"
        val refactorings: List<MoveMethodCandidateRefactoring> = getMoveMethodCandidates(fileName)
        UsefulTestCase.assertEquals(1, refactorings.size)
        val featureEnvyVisualizationData: FeatureEnvyVisualizationData = refactorings[0].visualizationData
        assertEquals("testMethod", featureEnvyVisualizationData.methodToBeMoved.name)
        assertEquals(2, featureEnvyVisualizationData.distinctTargetDependencies)
        assertEquals(0, featureEnvyVisualizationData.distinctSourceDependencies)
    }

    fun testSourceMethodInvocationsCount() {
        val fileName = "TestSourceMethodInvocationsCount.kt"
        val refactorings: List<MoveMethodCandidateRefactoring> = getMoveMethodCandidates(fileName)
        UsefulTestCase.assertEquals(1, refactorings.size)
        val featureEnvyVisualizationData: FeatureEnvyVisualizationData = refactorings[0].visualizationData
        assertEquals("testMethod", featureEnvyVisualizationData.methodToBeMoved.name)
        assertEquals(2, featureEnvyVisualizationData.distinctTargetDependencies)
        assertEquals(1, featureEnvyVisualizationData.distinctSourceDependencies)
    }

    fun testTargetClassIsInheritedByAnotherTargetClass() {
        val fileName = "TestTargetClassIsInheritedByAnotherTargetClass.kt"
        val refactorings: List<MoveMethodCandidateRefactoring> = getMoveMethodCandidates(fileName)
        UsefulTestCase.assertEquals(2, refactorings.size)
        val featureEnvyVisualizationDataBase: FeatureEnvyVisualizationData = refactorings[0].visualizationData
        val  featureEnvyVisualizationDataDerived: FeatureEnvyVisualizationData = refactorings[1].visualizationData
        assertEquals("testMethodBase", featureEnvyVisualizationDataBase.methodToBeMoved.name)
        assertEquals("testMethodDerived", featureEnvyVisualizationDataDerived.methodToBeMoved.name)
        assertEquals(2, featureEnvyVisualizationDataBase.distinctTargetDependencies)
        assertEquals(1, featureEnvyVisualizationDataDerived.distinctTargetDependencies)
    }

    fun testMethodCanBeMoved() {
        val fileName = "TestMethodCanBeMoved.kt"
        val refactorings: List<MoveMethodCandidateRefactoring> = getMoveMethodCandidates(fileName)
        UsefulTestCase.assertEquals(0, refactorings.size)
    }
}
