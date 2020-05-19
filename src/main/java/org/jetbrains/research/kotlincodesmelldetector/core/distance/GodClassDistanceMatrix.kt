package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import org.jetbrains.research.kotlincodesmelldetector.utils.fields
import org.jetbrains.research.kotlincodesmelldetector.utils.generateFullEntitySets
import org.jetbrains.research.kotlincodesmelldetector.utils.math.Clustering
import org.jetbrains.research.kotlincodesmelldetector.utils.methods
import org.jetbrains.research.kotlincodesmelldetector.utils.toPointer

private const val maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate = 2

var debugMode = false

fun getJaccardDistanceMatrix(entities: List<KtElement>): Array<DoubleArray> {
    val jaccardDistanceMatrix =
        Array(entities.size) { DoubleArray(entities.size) }
    val entitySets = generateFullEntitySets(entities)
    for (i in jaccardDistanceMatrix.indices) {
        for (j in jaccardDistanceMatrix.indices) {
            if (i != j) {
                jaccardDistanceMatrix[i][j] = DistanceCalculator.getDistance(
                    entitySets[entities[i]] ?: error("No entity set generated for ${entities[i]}"),
                    entitySets[entities[j]] ?: error("No entity set generated for ${entities[j]}")
                )
            } else {
                jaccardDistanceMatrix[i][j] = 0.0
            }
        }
    }

    return jaccardDistanceMatrix
}

fun getExtractClassCandidateRefactorings(
    projectInfo: ProjectInfo,
    classesToBeExamined: MutableList<SmartPsiElementPointer<KtElement>>,
    indicator: ProgressIndicator
): MutableList<ExtractClassCandidateRefactoring> {
    val candidateList: MutableList<ExtractClassCandidateRefactoring> = mutableListOf()

    indicator.text = KotlinCodeSmellDetectorBundle.message("god.class.identification.indicator")
    indicator.fraction = 0.0
    for (sourceClassPointer in classesToBeExamined) {
        val sourceClass = sourceClassPointer.element
        if (classIsBigEnough(sourceClass)) {
            val entities = mutableListOf<KtDeclaration>()
            entities.addAll(sourceClass.fields)
            entities.addAll(sourceClass.methods)

            val distanceMatrix: Array<DoubleArray> = getJaccardDistanceMatrix(entities)
            val clustering = Clustering.getInstance(0, distanceMatrix)
            val clusters = clustering.clustering(entities)
            var processedClusters = 0
            for (cluster in clusters) {
                processedClusters += 1
                indicator.fraction = processedClusters.toDouble() / clusters.size
                val candidate =
                    ExtractClassCandidateRefactoring(projectInfo, sourceClassPointer, cluster.entities)

                if (debugMode) {
                    candidateList.add(candidate)
                    continue
                }

                if (candidate.isApplicable) {
                    val sourceClassDependencies = candidate.distinctSourceDependencies
                    val extractedClassDependencies = candidate.distinctTargetDependencies

                    if (sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate &&
                        sourceClassDependencies < extractedClassDependencies
                    ) {
                        candidateList.add(candidate)
                    }
                }
            }
            // Clustering End
        }
    }

    indicator.fraction = 1.0
    return candidateList
}

private fun classIsBigEnough(sourceClass: KtElement?): Boolean {
    //TODO
    return true
}