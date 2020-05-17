package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import org.jetbrains.research.kotlincodesmelldetector.utils.*
import java.util.TreeMap

class DistanceMatrix(private val project: ProjectInfo, private val indicator: ProgressIndicator) {
    val classes: MutableMap<KtClassOrObject, ClassEntity> = mutableMapOf()
    private val classFqNames: MutableSet<FqName> = mutableSetOf()
    private val entityToClassMap = mutableMapOf<KtNamedDeclaration, MutableList<KtClassOrObject>>()
    private val entityMap = mutableMapOf<KtNamedDeclaration, MutableSet<KtNamedDeclaration>>()
    private val classMap = mutableMapOf<KtClassOrObject, Set<KtNamedDeclaration>>()
    private val maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate = 2

    init {
        generateEntitySets()

        for ((classOrObject, entitySet) in classMap.entries) {
            for (entity in entitySet) {
                if (entity in entityToClassMap.keys) {
                    entityToClassMap[entity]!!.add(classOrObject)
                } else {
                    entityToClassMap[entity] = mutableListOf(classOrObject)
                }
            }
        }
    }

    private fun generateEntitySets() {
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.parsing.indicator")
        indicator.isIndeterminate = false
        for ((i, pointer) in project.classes.withIndex()) {
            indicator.fraction = i.toDouble() / (2 * project.classes.size)
            val classOrObject = pointer.element as? KtClassOrObject ?: continue
            val fqName = classOrObject.fqName ?: continue
            if (!(classOrObject is KtClass && classOrObject.isEnum()) && !(classOrObject is KtObjectDeclaration && classOrObject.isCompanion())) {
                val classEntity = ClassEntity(classOrObject)
                if (!(classOrObject is KtClass && classOrObject.isData() && classEntity.methodList.size == 0)) {
                    classes[classOrObject] = classEntity
                    classFqNames.add(fqName)
                }
            }
        }
        indicator.fraction = 0.5
        for ((i, entry) in classes.entries.withIndex()) {
            val (classOrObject, classEntity) = entry
            indicator.fraction = 0.5 + i.toDouble() / (2 * classes.size)
            val classEntitySet: MutableSet<KtNamedDeclaration> = mutableSetOf()
            for (method in classEntity.methodList) {
                if (methodIsDelegate(method) == null && method !in entityMap) {
                    entityMap[method] = processReferencesInMethodBody(method)
                }
                classEntitySet.add(method)
            }
            for (attribute in classEntity.attributeList) {
                if (!attributeIsReference(attribute) && attribute !in entityMap) {
                    entityMap[attribute] = mutableSetOf()
                }
                classEntitySet.add(attribute)
            }
            classMap[classOrObject] = classEntitySet
        }
        indicator.fraction = 1.0
    }

    private fun processReferencesInMethodBody(method: KtNamedFunction): MutableSet<KtNamedDeclaration> {
        val methodEntitySet = mutableSetOf<KtNamedDeclaration>()
        method.bodyExpression?.forEachDescendantOfType<KtNameReferenceExpression> { reference ->
            reference.mainReference.resolve()?.let { called ->
                if (called is KtNamedDeclaration) {
                    val containingClass = called.containingClassOrObject
                    if (containingClass != null && containingClass in classes.keys) {
                        if (called is KtProperty || (called is KtParameter && called.hasValOrVar())) {
                            if (!attributeIsReference(called)) {
                                if (called !in entityMap) {
                                    entityMap[called] = mutableSetOf()
                                }
                                entityMap[called]!!.add(method)
                                methodEntitySet.add(called)
                            }
                        }
                        if (called is KtNamedFunction) {
                            if (methodIsDelegate(called) != null) {
                                getFinalNonDelegateMethod(called)?.let { nonDelegateMethod ->
                                    methodEntitySet.add(nonDelegateMethod)
                                }
                            } else {
                                methodEntitySet.add(called)
                            }
                        }

                    }
                }
            }
        }
        return methodEntitySet
    }

    fun getMoveMethodCandidateRefactoringsByAccess(
            classesToBeExamined: Set<KtClassOrObject>,
            indicator: ProgressIndicator
    ): List<MoveMethodCandidateRefactoring> {
        val candidateRefactoringList: MutableList<MoveMethodCandidateRefactoring> = mutableListOf()
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.identification.indicator")
        for ((i, entity) in entityMap.keys.withIndex()) {
            indicator.fraction = i.toDouble() / entityMap.keys.size
            if (entity is KtNamedFunction && methodCanBeMoved(entity)) {
                val sourceClass = entity.containingClassOrObject ?: continue
                if (classesToBeExamined.contains(sourceClass)) {
                    val entitySet = entityMap[entity]!!
                    val accessMap = computeAccessMap(entitySet)
                    val sortedByAccessMap = TreeMap<Int, MutableSet<KtClassOrObject>>()
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
                        val candidates = getCandidatesFromTargetClasses(entity, accessMap, sourceClass, targetClasses)
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
            accessMap: Map<KtClassOrObject, List<KtNamedDeclaration>>,
            sourceClass: KtClassOrObject,
            targetClasses: Set<KtClassOrObject>
    ): List<MoveMethodCandidateRefactoring> {
        val candidates = mutableListOf<MoveMethodCandidateRefactoring>()
        val candidateTargetClasses = mutableSetOf<KtClassOrObject>()
        val sourceClassEntity = classes[sourceClass] ?: return listOf()

        for (targetClass in targetClasses) {
            // TODO: additional methods to be moved
            val intersectionWithSourceClass = accessMap[sourceClass]?.size ?: 0
            val intersectionWithTargetClass = accessMap[targetClass]?.size ?: 0
            if (intersectionWithTargetClass >= intersectionWithSourceClass) {
                val targetClassEntity = classes[targetClass] ?: continue
                val candidate = MoveMethodCandidateRefactoring(project, sourceClassEntity, targetClassEntity, entity)
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

    private fun computeAccessMap(entitySet: Set<KtNamedDeclaration>): Map<KtClassOrObject, MutableList<KtNamedDeclaration>> {
        val accessMap: MutableMap<KtClassOrObject, MutableList<KtNamedDeclaration>> = mutableMapOf()
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
            candidateTargetClasses: Set<KtClassOrObject>
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
            superClass?.let {
                if (superClass !in classMap.keys) return false
                if (superClass in candidateTargetClasses) return true

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
                    if (genericType in classFqNames) {
                        return true
                    }
                }
            }
            if (type in classFqNames) {
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
                    val containingClass = callee.containingClassOrObject
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