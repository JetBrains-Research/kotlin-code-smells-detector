package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.research.kotlincodesmelldetector.utils.TopicFinder
import java.util.ArrayList
import java.util.HashSet
import java.util.TreeSet

class ExtractedConcept(
    private val conceptEntities: Set<SmartPsiElementPointer<out KtElement>>
) :
    Comparable<ExtractedConcept?> {

    val conceptClusters: MutableSet<ExtractClassCandidateRefactoring> = HashSet()

    var topics: List<String> = ArrayList()
        private set

    fun addConceptCluster(candidate: ExtractClassCandidateRefactoring) {
        conceptClusters.add(candidate)
    }

    fun findTopics() {
        val codeElements = ArrayList<String>()
        for (entity in conceptEntities) {
            if (entity.element is KtNamedDeclaration) {
                codeElements.add(entity.element?.name ?: "")
            }
        }

        topics = TopicFinder.findTopics(codeElements)
    }

    override operator fun compareTo(other: ExtractedConcept?): Int {
        if (other == null) {
            return 1
        }

        val thisSet = TreeSet(conceptClusters)
        val otherSet = TreeSet(other.conceptClusters)

        val thisFirst = thisSet.first()
        val otherFirst = otherSet.first()
        return thisFirst.compareTo(otherFirst)
    }

    override fun toString(): String {
        return topics.toString()
    }
}