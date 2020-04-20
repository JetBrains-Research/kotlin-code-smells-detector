package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtElement
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.TreeSet

class ExtractClassCandidateGroup(val source: SmartPsiElementPointer<KtElement>) :
    Comparable<ExtractClassCandidateGroup?> {
    private val _candidates: ArrayList<ExtractClassCandidateRefactoring> = ArrayList()
    val extractedConcepts: ArrayList<ExtractedConcept> = ArrayList<ExtractedConcept>()

    val candidates: ArrayList<ExtractClassCandidateRefactoring>
        get() {
            _candidates.sort()
            return _candidates
        }

    fun addCandidate(candidate: ExtractClassCandidateRefactoring) {
        _candidates.add(candidate)
    }

    fun groupConcepts() {
        val tempCandidates = mutableListOf<ExtractClassCandidateRefactoring>()
        tempCandidates.addAll(_candidates)
        tempCandidates.sortWith(ClusterSizeComparator())
        while (tempCandidates.isNotEmpty()) {
            val conceptEntities = HashSet(tempCandidates[0].extractedEntities)
            val indexSet: MutableSet<Int> = LinkedHashSet()
            indexSet.add(0)
            var previousSize: Int
            do {
                previousSize = conceptEntities.size
                for (i in 1 until tempCandidates.size) {
                    val copiedConceptEntities = HashSet(conceptEntities)
                    copiedConceptEntities.retainAll(tempCandidates[i].extractedEntities)
                    if (copiedConceptEntities.isNotEmpty()) {
                        conceptEntities.addAll(tempCandidates[i].extractedEntities)
                        indexSet.add(i)
                    }
                }
            } while (previousSize < conceptEntities.size)
            val candidatesToBeRemoved: MutableSet<ExtractClassCandidateRefactoring?> =
                HashSet()
            val newConcept = ExtractedConcept(conceptEntities)
            for (j in indexSet) {
                newConcept.addConceptCluster(tempCandidates[j])
                candidatesToBeRemoved.add(tempCandidates[j])
            }
            tempCandidates.removeAll(candidatesToBeRemoved)
            extractedConcepts.add(newConcept)
        }
        findConceptTerms()
    }

    private fun findConceptTerms() {
        for (concept in extractedConcepts) {
            concept.findTopics()
            for (conceptCluster in concept.conceptClusters) {
                conceptCluster.findTopics()
            }
        }
    }

    override operator fun compareTo(other: ExtractClassCandidateGroup?): Int {
        if (other == null) {
            return 1
        }

        val thisSet = TreeSet(_candidates)
        val otherSet = TreeSet(other._candidates)
        val thisFirst = thisSet.first()
        val otherFirst = otherSet.first()
        val comparison = thisFirst.compareTo(otherFirst)
        return if (comparison != 0) {
            comparison
        } else {
            source.element.toString().compareTo(other.source.element.toString())
        }
    }
}