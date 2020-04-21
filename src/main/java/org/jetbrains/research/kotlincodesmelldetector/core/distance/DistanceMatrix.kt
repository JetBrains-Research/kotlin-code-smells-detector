package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import org.jetbrains.research.kotlincodesmelldetector.utils.getConstructorType
import org.jetbrains.research.kotlincodesmelldetector.utils.getGenericType
import org.jetbrains.research.kotlincodesmelldetector.utils.isContainer
import org.jetbrains.research.kotlincodesmelldetector.utils.nameWithParameterList
import java.util.*
import kotlin.collections.ArrayList

class DistanceMatrix(private val project: ProjectInfo, private val indicator: ProgressIndicator) {
    val classes: MutableMap<FqName, ClassEntity> = mutableMapOf()
    private val entityIndexMap = mutableMapOf<FqName, Int>()
    private val classIndexMap = mutableMapOf<FqName, Int>()
    private val classList = mutableListOf<ClassEntity>()
    private val entityList = mutableListOf<KtNamedDeclaration>()
    private val entityMap = mutableMapOf<FqName, MutableSet<FqName>>()
    private val classMap = mutableMapOf<FqName, Set<FqName>>()
    private val maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate = 2

    init {
        generateEntitySets()
        generateDistances()
    }

    private fun generateEntitySets() {
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.parsing.indicator")
        indicator.fraction = 0.0
        for ((i, ktClassPointer) in project.classes.withIndex()) {
            val ktClass = ktClassPointer.element!!
            if (ktClass is KtClassOrObject) {
                if (!(ktClass is KtClass && ktClass.isEnum())) {
                    val classEntity = ClassEntity(ktClass)
                    classes[classEntity.fqName] = classEntity
                }
            }
            indicator.fraction = (i + 1).toDouble() / (2 * project.classes.size)
        }
        for ((i, myClass) in classes.values.withIndex()) {
            val classEntitySet: MutableSet<FqName> = mutableSetOf()
            for (method in myClass.methodList) {
                if (methodIsDelegate(method) == null && method.nameWithParameterList !in entityMap) {
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
                                            if (methodIsDelegate(called) != null) {
                                                val nonDelegateMethod = getFinalNonDelegateMethod(called)
                                                nonDelegateMethod?. let {
                                                    methodEntitySet.add(nonDelegateMethod.nameWithParameterList)
                                                }
                                            }
                                            else {
                                                methodEntitySet.add(called.nameWithParameterList)
                                            }
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
            classMap[myClass.fqName] = classEntitySet
            indicator.fraction = 0.5 + (i + 1).toDouble() / (2 * classes.size)
        }
        indicator.fraction = 1.0
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
            val className = myClass.fqName
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
                                if (intersectionWithTargetClass.size >= intersectionWithSourceClass.size) {
                                    if (candidate.isApplicable() && !targetClassInheritedByAnotherCandidateTargetClass(targetClass, accessMap.keys)) {
                                        val sourceClassDependencies = candidate.distinctSourceDependencies
                                        val targetClassDependencies = candidate.distinctTargetDependencies
                                        if (sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate
                                                && sourceClassDependencies < targetClassDependencies) {
                                            candidateRefactoringList.add(candidate)
                                            candidateFound = true
                                        }
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


    private fun attributeIsReference(attribute: KtNamedDeclaration): Boolean {
        val type = getConstructorType(attribute)
        type?.let {
            if (isContainer(type)) {
                val genericType = getGenericType(attribute)
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

    private fun methodIsDelegate(method: KtNamedFunction): KtNamedFunction? {
        method.bodyExpression?.let { body ->
            var callExpression: KtCallExpression? = null
            if (body is KtCallExpression) {
                callExpression = body
            } else if (!(body is KtBlockExpression && body.children.size != 1)) {
                val callExpressions: List<KtCallExpression> = body.collectDescendantsOfType()
                if (callExpressions.size == 1) {
                    callExpression = callExpressions[0]
                }
            }
            callExpression?.calleeExpression?.mainReference?.resolve()?.let { callee ->
                if (callee is KtNamedFunction) {
                    val containingClass = callee.containingClassOrObject?.fqName
                    if (containingClass != null && containingClass in classes.keys)
                        return callee
                }
            }
        }
        return null
    }

    private fun getFinalNonDelegateMethod(method: KtNamedFunction): KtNamedFunction? {
        val delegateSet: MutableSet<KtNamedFunction> = mutableSetOf()
        var delegate: KtNamedFunction = method
        while (delegate !in delegateSet) {
            val callee = methodIsDelegate(delegate) ?: return delegate
            delegateSet.add(delegate)
            delegate = callee
        }
        return null
    }
}