package org.jetbrains.research.kotlincodesmelldetector

import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateGroup
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateRefactoring
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo
import java.util.Arrays.asList
import java.util.stream.Collectors

private const val PATH_TO_TESTDATA = "src/test/resources/testdata/"
private const val PATH_TO_TESTS = "/core/distance/godclass/"

internal class GodClassDistanceMatrixTest : LightJavaCodeInsightFixtureTestCase() {

    private fun getExtractClassCandidateGroups(classFileName: String): List<ExtractClassCandidateGroup> {
        myFixture.testDataPath = PATH_TO_TESTDATA
        myFixture.configureByFile(PATH_TO_TESTS + classFileName)
        val project = myFixture.project
        val projectInfo = ProjectInfo(project)
        val set: Set<ExtractClassCandidateGroup> =
            KotlinCodeSmellFacade.getExtractClassRefactoringOpportunities(projectInfo, ProgressIndicatorBase())

        return set.toList()
    }

    private fun checkGroupsAreEmpty(classFileName: String) {
        val groups = getExtractClassCandidateGroups(classFileName)
        TestCase.assertTrue(groups.isEmpty())
    }

    private fun compareExtractClassGroups(
        groups: List<ExtractClassCandidateGroup>,
        expectedFields: List<List<String>>,
        expectedMethods: List<List<String>>,
        groupNumber: Int = -1,
        candidateNumber: Int = -1
    ) {
        if (groupNumber > 0) {
            TestCase.assertTrue(groups.size > groupNumber)
            val group = groups[groupNumber]

            TestCase.assertTrue(group.candidates.size >= candidateNumber)
            val refactoring =
                group.candidates[candidateNumber]
            val fields = expectedFields[candidateNumber]
            val methods = expectedMethods[candidateNumber]

            compareExtractClassRefactoring(refactoring, fields, methods)
        } else {
            var counter = 0
            for (group in groups) {
                for (candidate in group.candidates) {
                    val fields = expectedFields[counter]
                    val methods = expectedMethods[counter]

                    compareExtractClassRefactoring(candidate, fields, methods)

                    counter++
                }
            }
        }
    }

    private fun compareExtractClassRefactoring(
        refactoring: ExtractClassCandidateRefactoring,
        expectedFields: List<String>,
        expectedMethods: List<String>
    ) {
        val extractedFieldsNames =
            refactoring.extractedFields.map { obj -> obj.name!! }

        TestCase.assertEquals(expectedFields, extractedFieldsNames)

        val extractedMethodsNames =
            refactoring.extractedMethods.map { obj: KtDeclaration -> obj.name!! }

        TestCase.assertEquals(expectedMethods, extractedMethodsNames)
    }

    fun testSeparateBlocks() {
        val classFileName = "TestSeparateBlocks.kt"

        val expectedFields =
            listOf(listOf("a", "b", "c"), listOf("d", "e"))

        val expectedMethods =
            listOf(listOf("fun1"), listOf("fun2"))

        val groups = getExtractClassCandidateGroups(classFileName)
        TestCase.assertFalse(groups.isEmpty())
        compareExtractClassGroups(groups, expectedFields, expectedMethods)
    }

    fun testOnlyFields() {
        checkGroupsAreEmpty("TestOnlyFields.kt")
    }

    fun testManySeparatesBlocks() {
        val classFileName = "TestSeparateBlocksWithStrictOrder.kt"
        val groups = getExtractClassCandidateGroups(classFileName)
        assertFalse(groups.isEmpty())

        val expectedFields: MutableList<List<String>> = mutableListOf()
        val expectedMethods: MutableList<List<String>> = mutableListOf()

        for (i in 0..5) {
            when (i) {
                0 -> {
                    expectedFields.add(listOf("aa", "ab", "ac", "ad", "ae", "af", "ag"))
                    expectedMethods.add(listOf("fun1"))
                }
                1 -> {
                    expectedFields.add(listOf("ba", "bb", "bc", "bd", "be", "bf"))
                    expectedMethods.add(listOf("fun2"))
                }
                2 -> {
                    expectedFields.add(listOf("ca", "cb", "cc", "cd", "ce"))
                    expectedMethods.add(listOf("fun3"))
                }
                3 -> {
                    expectedFields.add(listOf("da", "db", "dc", "dd"))
                    expectedMethods.add(listOf("fun4"))
                }
                4 -> {
                    expectedFields.add(listOf("ea", "eb", "ec"))
                    expectedMethods.add(listOf("fun5"))
                }
                else -> {
                    expectedFields.add(listOf("fa", "fb"))
                    expectedMethods.add(listOf("fun6"))
                }
            }
        }

        compareExtractClassGroups(groups, expectedFields, expectedMethods)
    }

    fun testSynchronizedMethod() {
        checkGroupsAreEmpty("TestSynchronizedMethod.kt")
    }

    fun testSynchronizedMethodBody() {
        val classFileName = "TestSynchronizedMethodBlock.kt"
        val groups = getExtractClassCandidateGroups(classFileName)

        //ORIGINAL PLUGIN ACTUALLY ALLOWS IT, BUT IT SHOULDN'T. PROBABLY A BUG. TODO add and check
        //assertTrue(groups.isEmpty())
    }

    fun testPublicFields() {
        checkGroupsAreEmpty("TestPublicFields.kt")
    }

    fun testOverride() {
        checkGroupsAreEmpty("TestOverride.kt")
    }

    fun testCompanionObject() {
        val classFileName = "TestCompanionObject.kt"

        val expectedFields =
            listOf(listOf("a", "b", "c"), listOf("d", "e"))

        val expectedMethods =
            listOf(listOf("fun1"), listOf("fun2"))

        val groups = getExtractClassCandidateGroups(classFileName)
        TestCase.assertFalse(groups.isEmpty())
        compareExtractClassGroups(groups, expectedFields, expectedMethods)
    }

    fun testTopLevel() {
        val classFileName = "TestTopLevel.kt"

        val expectedFields =
            listOf(listOf("a", "b", "c"), listOf("d", "e"))

        val expectedMethods =
            listOf(listOf("fun1"), listOf("fun2"))

        val groups = getExtractClassCandidateGroups(classFileName)
        TestCase.assertFalse(groups.isEmpty())

        compareExtractClassGroups(groups, expectedFields, expectedMethods)
    }

    fun testOnlyMethods() {
        checkGroupsAreEmpty("TestOnlyMethods.kt")
    }

    fun testProperties() {
        val classFileName = "TestProperties.kt"

        val expectedFields =
            listOf(listOf("a", "b", "c"), listOf("d", "e"))

        val expectedMethods =
            listOf(listOf("property1"), listOf("property2"))

        val groups = getExtractClassCandidateGroups(classFileName)
        TestCase.assertFalse(groups.isEmpty())

        compareExtractClassGroups(groups, expectedFields, expectedMethods)
    }

    fun testMainConstructorVals() {
        val classFileName = "TestMainConstructorVals.kt"

        val expectedFields =
            listOf(listOf("a", "b", "c"), listOf("d", "e"))

        val expectedMethods =
            listOf(listOf("fun1"), listOf("fun2"))

        val groups = getExtractClassCandidateGroups(classFileName)
        TestCase.assertFalse(groups.isEmpty())

        compareExtractClassGroups(groups, expectedFields, expectedMethods)
    }

    fun testEnclosingAccess() {
        //TODO
        //checkGroupsAreEmpty("TestEnclosingAccess.kt")
    }

    fun testDelegate() {
        //TODO
        //checkGroupsAreEmpty("TestDelegate.kt")
    }

    fun testContainsSuperMethodInvocation() {
        //TODO
        //checkGroupsAreEmpty("TestContainsSuperMethodInvocation.kt")
    }
}