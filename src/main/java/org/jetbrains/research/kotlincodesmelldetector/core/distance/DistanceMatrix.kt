package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import org.jetbrains.research.kotlincodesmelldetector.utils.getConstructorType
import org.jetbrains.research.kotlincodesmelldetector.utils.getFirstTypeArgumentType
import org.jetbrains.research.kotlincodesmelldetector.utils.isContainer
import org.jetbrains.research.kotlincodesmelldetector.utils.isOpen
import org.jetbrains.research.kotlincodesmelldetector.utils.isSynchronized
import org.jetbrains.research.kotlincodesmelldetector.utils.overridesMethod
import org.jetbrains.research.kotlincodesmelldetector.utils.signature
import java.util.TreeMap

class DistanceMatrix(private val project: ProjectInfo, private val indicator: ProgressIndicator) {
    val classes: MutableMap<String, ClassEntity> = mutableMapOf()
    private val entityToClassMap = mutableMapOf<String, MutableList<String>>()
    private val classIndexMap = mutableMapOf<String, Int>()
    private val classList = mutableListOf<ClassEntity>()
    private val entityList = mutableListOf<KtNamedDeclaration>()
    private val entityMap = mutableMapOf<String, MutableSet<String>>()
    private val classMap = mutableMapOf<String, Set<String>>()
    private val maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate = 2

    init {
        generateEntitySets()

        val classSignatures = arrayOfNulls<String>(classList.size)
        for ((j, classEntity) in classList.withIndex()) {
            val classSignature = classEntity.signature
            classSignatures[j] = classSignature
            if (!classIndexMap.containsKey(classSignature))
                classIndexMap[classSignature] = j
        }

        for ((classSignature, entitySet) in classMap.entries) {
            for (entitySignature in entitySet) {
                if (entitySignature in entityToClassMap.keys) {
                    entityToClassMap[entitySignature]!!.add(classSignature)
                } else {
                    entityToClassMap[entitySignature] = mutableListOf(classSignature)
                }
            }
        }
    }

    private fun generateEntitySets() {
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.parsing.indicator")
        for ((i, ktClassPointer) in project.classes.withIndex()) {
            indicator.fraction = i.toDouble() / (2 * project.classes.size)
            val ktClass = ktClassPointer.element ?: continue
            val classSignature = (ktClass as? KtClassOrObject)?.signature ?: continue
            if (!(ktClass is KtClass && ktClass.isEnum()) && !(ktClass is KtObjectDeclaration && ktClass.isCompanion())) {
                val classEntity = ClassEntity(ktClass, classSignature)
                if (!(ktClass is KtClass && ktClass.isData() && classEntity.methodList.size == 0)) {
                    classes[classEntity.signature] = classEntity
                }
            }
        }
        indicator.fraction = 0.5
        for ((i, myClass) in classes.values.withIndex()) {
            indicator.fraction = 0.5 + i.toDouble() / (2 * classes.size)
            val classEntitySet: MutableSet<String> = mutableSetOf()
            for (method in myClass.methodList) {
                val methodSignature = method.signature ?: continue
                if (methodIsDelegate(method) == null && method.signature !in entityMap) {
                    entityMap[methodSignature] = processReferencesInMethodBody(method, methodSignature)
                    entityList.add(method)
                }
                classEntitySet.add(methodSignature)
            }
            for (attribute in myClass.attributeList) {
                val attributeSignature = attribute.signature ?: continue
                if (!attributeIsReference(attribute) && attributeSignature !in entityMap) {
                    entityMap[attributeSignature] = mutableSetOf()
                    entityList.add(attribute)
                }
                classEntitySet.add(attributeSignature)
            }
            classList.add(myClass)
            classMap[myClass.signature] = classEntitySet
        }
        indicator.fraction = 1.0
    }

    private fun processReferencesInMethodBody(method: KtNamedFunction, methodSignature: String): MutableSet<String> {
        val methodEntitySet = mutableSetOf<String>()
        method.bodyExpression?.forEachDescendantOfType<KtNameReferenceExpression> { reference ->
            reference.mainReference.resolve()?.let { called ->
                if (called is KtNamedDeclaration) {
                    val containingClass = called.containingClassOrObject?.signature
                    if (containingClass != null && containingClass in classes.keys) {
                        called.signature?.let { calledSignature ->
                            if (called is KtProperty || (called is KtParameter && called.hasValOrVar())) {
                                if (!attributeIsReference(called)) {
                                    called.containingClassOrObject
                                    if (calledSignature in entityMap) {
                                        entityMap[calledSignature]!!.add(methodSignature)
                                    } else {
                                        entityMap[calledSignature] = mutableSetOf(methodSignature)
                                        entityList.add(called)
                                    }
                                    methodEntitySet.add(calledSignature)
                                }
                            }
                            if (called is KtNamedFunction) {
                                if (methodIsDelegate(called) != null) {
                                    val nonDelegateMethod = getFinalNonDelegateMethod(called)
                                    nonDelegateMethod?.signature?.let { nonDelegateMethodSignature ->
                                        methodEntitySet.add(nonDelegateMethodSignature)
                                    }
                                } else {
                                    methodEntitySet.add(calledSignature)
                                }
                            }
                        }
                    }
                }
            }
        }
        return methodEntitySet
    }

    fun getMoveMethodCandidateRefactoringsByAccess(
        classNamesToBeExamined: Set<String>,
        indicator: ProgressIndicator
    ): List<MoveMethodCandidateRefactoring> {
        val candidateRefactoringList: MutableList<MoveMethodCandidateRefactoring> = mutableListOf()
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.identification.indicator")
        for ((i, entity) in entityList.withIndex()) {
            indicator.fraction = i.toDouble() / entityList.size
            if (entity is KtNamedFunction && methodCanBeMoved(entity)) {
                val sourceClass: String = entity.containingClassOrObject?.signature ?: continue
                if (classNamesToBeExamined.contains(sourceClass)) {
                    val entitySet = entityMap[entity.signature]!!
                    val accessMap = computeAccessMap(entitySet)
                    val sortedByAccessMap = TreeMap<Int, MutableSet<String>>()
                    for (targetClass in accessMap.keys) {
                        val numberOfAccessedEntities = accessMap[targetClass]?.size ?: 0
                        if (sortedByAccessMap.containsKey(numberOfAccessedEntities)) {
                            sortedByAccessMap[numberOfAccessedEntities]!!.add(targetClass)
                        } else {
                            sortedByAccessMap[numberOfAccessedEntities] = mutableSetOf(targetClass)
                        }
                    }
                    while (sortedByAccessMap.isNotEmpty()) {
                        val targetClasses = sortedByAccessMap[sortedByAccessMap.lastKey()]!!
                        if (sourceClass in targetClasses) break
                        val candidates = getCandidatesFromTargetClasses(entity, entitySet, sourceClass, targetClasses)
                        if (candidates.isNotEmpty()) {
                            candidateRefactoringList.addAll(candidates)
                            break
                        }
                        sortedByAccessMap.remove(sortedByAccessMap.lastKey())
                    }
                }
            }
        }
        indicator.fraction = 1.0
        return candidateRefactoringList
    }

    private fun getCandidatesFromTargetClasses(
        entity: KtNamedFunction,
        entitySet: Set<String>,
        sourceClass: String,
        targetClasses: Set<String>
    ): List<MoveMethodCandidateRefactoring> {
        val candidates = mutableListOf<MoveMethodCandidateRefactoring>()
        val candidateTargetClasses = mutableSetOf<String>()
        for (targetClass in targetClasses) {
            val sourceClassEntity = classList[classIndexMap[sourceClass]!!]
            val targetClassEntity = classList[classIndexMap[targetClass]!!]
            val candidate = MoveMethodCandidateRefactoring(project, sourceClassEntity, targetClassEntity, entity)
            // TODO: additional methods to be moved
            val sourceClassEntitySet = classMap[sourceClass]!!
            val targetClassEntitySet = classMap[targetClass]!!
            val intersectionWithSourceClass = entitySet.intersect(sourceClassEntitySet).toMutableSet()
            val intersectionWithTargetClass = entitySet.intersect(targetClassEntitySet).toMutableSet()
            if (intersectionWithTargetClass.size >= intersectionWithSourceClass.size) {
                if (candidate.isApplicable()) {
                    val sourceClassDependencies = candidate.distinctSourceDependencies
                    val targetClassDependencies = candidate.distinctTargetDependencies
                    if (sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate
                        && sourceClassDependencies < targetClassDependencies
                    ) {
                        candidates.add(candidate)
                        candidateTargetClasses.add(targetClass)
                    }
                }
            }
        }
        return candidates.filter { !targetClassIsInheritedByAnotherTargetClass(it.targetClass, candidateTargetClasses) }
    }

    private fun computeAccessMap(entitySet: Set<String>): Map<String, MutableList<String>> {
        val accessMap: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (entity in entitySet) {
            for (containingClass in entityToClassMap[entity] ?: mutableListOf()) {
                if (accessMap.containsKey(containingClass)) {
                    accessMap[containingClass]!!.add(entity)
                } else {
                    accessMap[containingClass] = mutableListOf(entity)
                }
            }
        }
        return accessMap
    }

    private fun methodCanBeMoved(method: KtNamedFunction): Boolean {
        return !method.overridesMethod && !method.isSynchronized && !method.isOpen
    }

    private fun targetClassIsInheritedByAnotherTargetClass(
        targetClass: ClassEntity,
        candidateTargetClasses: Set<String>
    ): Boolean {
        var currentClass: KtClassOrObject? = targetClass.element
        while (currentClass != null) {
            var superClass: KtClassOrObject? = null
            for (superTypeEntry in currentClass.superTypeListEntries) {
                superClass =
                    superTypeEntry.typeAsUserType?.referenceExpression?.mainReference?.resolve() as? KtClassOrObject
                        ?: continue
                break
            }
            superClass?.signature?.let { superClassSignature ->
                if (superClassSignature !in classMap.keys) return false
                if (superClassSignature in candidateTargetClasses) return true

            }
            currentClass = superClass
        }
        return false
    }

    private fun attributeIsReference(attribute: KtNamedDeclaration): Boolean {
        val type = getConstructorType(attribute)
        type?.let {
            if (isContainer(type)) {
                val genericType = getFirstTypeArgumentType(attribute)
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
                    val containingClass = callee.containingClassOrObject?.signature
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