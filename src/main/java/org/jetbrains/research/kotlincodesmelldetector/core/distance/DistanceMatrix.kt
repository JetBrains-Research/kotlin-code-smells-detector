package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import java.util.*
import kotlin.collections.ArrayList

class DistanceMatrix(private val project: ProjectInfo) {
    val classes: MutableMap<FqName, ClassEntity> = mutableMapOf()
    private val entityIndexMap = mutableMapOf<FqName, Int>()
    private val classIndexMap = mutableMapOf<FqName, Int>()
    private val classList = mutableListOf<ClassEntity>()
    private val entityList = mutableListOf<KtNamedDeclaration>()
    private val entityMap = mutableMapOf<FqName, MutableSet<FqName>>()
    private val classMap = mutableMapOf<FqName, Set<FqName>>()
    private val acceptableOriginClassNames = mutableListOf<String>()
    private val maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate = 2

    init {
        addAcceptableOriginClassNames()
        generateEntitySets()
        generateDistances()
    }

    private fun generateEntitySets() {
        val ktClasses = project.classes
        for (ktClassPointer in ktClasses) {
            val ktClass = ktClassPointer.element!!
            if (!(ktClass is KtClass && ktClass.isEnum())) {
                val myClass = ClassEntity(ktClass)
                classes[myClass.element.fqName!!] = myClass
            }
        }
        for (myClass in classes.values) {
            val classEntitySet: MutableSet<FqName> = mutableSetOf()
            for (method in myClass.methodList) {
                if (method.nameWithParameterList !in entityMap) {
                    val methodEntitySet: MutableSet<FqName> = mutableSetOf()
                    method.bodyExpression?.forEachDescendantOfType<KtNameReferenceExpression> { reference ->
                        reference.mainReference.resolve()?.let { called ->
                            if (called is KtNamedDeclaration) {
                                val containingClass = called.containingClassOrObject?.fqName
                                if (containingClass != null && containingClass in classes.keys) {
                                    called.fqName?.let { fqName ->
                                        if (called is KtProperty || (called is KtParameter && called.hasValOrVar())) {
                                            if (!attributeIsReference(called)) {
                                                called.containingClassOrObject
                                                if (entityMap.contains(fqName)) {
                                                    entityMap[fqName]!!.add(method.nameWithParameterList)
                                                } else {
                                                    entityMap[fqName] = mutableSetOf(method.nameWithParameterList)
                                                    entityList.add(called)
                                                }
                                                methodEntitySet.add(called.fqName!!)
                                            }
                                        }
                                        if (called is KtNamedFunction) {
                                            // TODO: isDelegate
                                            methodEntitySet.add(called.nameWithParameterList)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    entityMap[method.nameWithParameterList] = methodEntitySet
                    entityList.add(method)
                }
                classEntitySet.add(method.nameWithParameterList)
            }
            for (attribute in myClass.attributeList) {
                if (!attributeIsReference(attribute)) {
                    if (attribute.fqName!! !in entityMap) {
                        entityMap[attribute.fqName!!] = mutableSetOf()
                        entityList.add(attribute)
                    }
                }
                classEntitySet.add(attribute.fqName!!)
            }
            classList.add(myClass)
            classMap[myClass.element.fqName!!] = classEntitySet
        }
    }

    private fun generateDistances() {
        val entityNames = arrayOfNulls<FqName>(entityList.size)
        val classNames = arrayOfNulls<FqName>(classList.size)

        for ((i, entity) in entityList.withIndex()) {
            val entityName = if (entity is KtNamedFunction) entity.nameWithParameterList else entity.getKotlinFqName()!!
            entityNames[i] = entityName
            entityIndexMap[entityName] = i
        }
        for ((j, myClass) in classList.withIndex()) {
            val className = myClass.element.fqName!!
            classNames[j] = className
            if (!classIndexMap.containsKey(className))
                classIndexMap[className] = j
        }
    }

    fun getMoveMethodCandidateRefactoringsByAccess(classNamesToBeExamined: Set<FqName>, indicator: ProgressIndicator): List<MoveMethodCandidateRefactoring> {
        val candidateRefactoringList: MutableList<MoveMethodCandidateRefactoring> = mutableListOf()
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.identification.indicator")
        indicator.fraction = 0.0
        val entityCount = entityList.size
        var processedEntities = 0
        for (entity in entityList) {
            processedEntities += 1
            if (entity is KtNamedFunction) {
                val sourceClass: FqName = entity.containingClassOrObject!!.fqName!!
                if (classNamesToBeExamined.contains(sourceClass)) {
                    val entitySet = entityMap[entity.nameWithParameterList]!!
                    val accessMap = computeAccessMap(entitySet)
                    val sortedByAccessMap = TreeMap<Int, MutableList<FqName>>()
                    for (targetClass in accessMap.keys) {
                        val numberOfAccessedEntities = accessMap[targetClass]!!.size
                        if (sortedByAccessMap.containsKey(numberOfAccessedEntities)) {
                            sortedByAccessMap[numberOfAccessedEntities]!!.add(targetClass)
                        } else {
                            sortedByAccessMap[numberOfAccessedEntities] = mutableListOf(targetClass)
                        }
                    }
                    var candidateFound = false
                    var sourceClassIsTarget = false
                    while (!candidateFound && !sourceClassIsTarget && !sortedByAccessMap.isEmpty()) {
                        val targetClasses = sortedByAccessMap[sortedByAccessMap.lastKey()]!!
                        for (targetClass in targetClasses) {
                            if (sourceClass == targetClass) {
                                sourceClassIsTarget = true
                            } else {
                                val mySourceClass = classList[classIndexMap[sourceClass]!!]
                                val myTargetClass = classList[classIndexMap[targetClass]!!]
                                val candidate = MoveMethodCandidateRefactoring(project, mySourceClass, myTargetClass, entity)
                                // TODO: additional methods to be moved
                                val methodEntitySet = entityMap[entity.nameWithParameterList]!!
                                val sourceClassEntitySet = classMap[sourceClass]!!
                                val targetClassEntitySet = classMap[targetClass]!!
                                val intersectionWithSourceClass: MutableSet<FqName> = methodEntitySet.intersect(sourceClassEntitySet).toMutableSet()
                                val intersectionWithTargetClass: MutableSet<FqName> = methodEntitySet.intersect(targetClassEntitySet).toMutableSet()
                                candidate.intersectionWithSourceClass = intersectionWithSourceClass.size
                                candidate.intersectionWithTargetClass = intersectionWithTargetClass.size
                                if (intersectionWithTargetClass.size >= intersectionWithSourceClass.size) {
                                    if (candidate.isApplicable() && !targetClassInheritedByAnotherCandidateTargetClass(targetClass, accessMap.keys)) {
                                        // TODO: distinctDependencies
                                        candidateRefactoringList.add(candidate)
                                        candidateFound = true
                                    }
                                }
                            }
                        }
                        sortedByAccessMap.remove(sortedByAccessMap.lastKey())
                    }
                }

            }
            indicator.fraction = processedEntities.toDouble() / entityCount
        }
        indicator.fraction = 1.0
        return candidateRefactoringList
    }

    private fun targetClassInheritedByAnotherCandidateTargetClass(targetClass: FqName, candidateTargetClasses: Set<FqName>): Boolean {
        // TODO: not yet implemented
        return false
    }

    private fun computeAccessMap(entitySet: Set<FqName>): Map<FqName, ArrayList<FqName>> {
        val accessMap: MutableMap<FqName, ArrayList<FqName>> = mutableMapOf()
        for (entity in entitySet) {
            val classOrigin = entity.parent()
            if (accessMap.containsKey(classOrigin)) {
                val list = accessMap[classOrigin]!!
                list.add(entity)
            } else {
                val list = ArrayList<FqName>()
                list.add(entity)
                if (classMap.containsKey(classOrigin))
                    accessMap[classOrigin] = list
            }
        }

        // TODO: superClass
        return accessMap
    }

    private fun addAcceptableOriginClassNames() {
        acceptableOriginClassNames.add("kotlin.collections.Collection")
        acceptableOriginClassNames.add("kotlin.collections.MutableCollection")
        acceptableOriginClassNames.add("kotlin.collections.AbstractCollection")
        acceptableOriginClassNames.add("kotlin.collections.List")
        acceptableOriginClassNames.add("kotlin.collections.MutableList")
        acceptableOriginClassNames.add("kotlin.collections.AbstractList")
        acceptableOriginClassNames.add("kotlin.collections.ArrayList")
        acceptableOriginClassNames.add("java.util.LinkedList")
        acceptableOriginClassNames.add("kotlin.collections.Set")
        acceptableOriginClassNames.add("kotlin.collections.MutableSet")
        acceptableOriginClassNames.add("java.util.AbstractSet")
        acceptableOriginClassNames.add("java.util.HashSet")
        acceptableOriginClassNames.add("java.util.LinkedHashSet")
        acceptableOriginClassNames.add("java.util.SortedSet")
        acceptableOriginClassNames.add("java.util.TreeSet")
        acceptableOriginClassNames.add("java.util.Vector")
        acceptableOriginClassNames.add("java.util.Stack")
    }

    private fun attributeIsReference(attribute: KtNamedDeclaration): Boolean {
        val type = attribute.type()?.constructor?.declarationDescriptor?.fqNameOrNull()
        type?.let {
            if (type.toString() in acceptableOriginClassNames) {
                val genericType = attribute.type()?.arguments?.getOrNull(0)
                        ?.type?.constructor?.declarationDescriptor?.fqNameOrNull()
                genericType?.let {
                    if (genericType in classes.keys) {
                        return true
                    }
                }
            }
            if (type in classes.keys) {
                return true
            }
        }
        return false
    }
}

val KtNamedFunction.nameWithParameterList: FqName
    get() {
        return FqName(this.fqName.toString()
                + this.valueParameters.map { "${it.name}: ${it.type()?.toString()}" }
                .joinToString(", ", "(", ")"))
    }