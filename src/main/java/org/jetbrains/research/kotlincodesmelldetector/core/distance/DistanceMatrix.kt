package org.jetbrains.research.kotlincodesmelldetector.core.distance

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle
import org.jetbrains.research.kotlincodesmelldetector.utils.*
import java.util.*
import kotlin.collections.ArrayList

class DistanceMatrix(private val project: ProjectInfo, private val indicator: ProgressIndicator) {
    val classes: MutableMap<String, ClassEntity> = mutableMapOf()
    private val entityIndexMap = mutableMapOf<String, Int>()
    private val classIndexMap = mutableMapOf<String, Int>()
    private val classList = mutableListOf<ClassEntity>()
    private val entityList = mutableListOf<KtNamedDeclaration>()
    private val entityMap = mutableMapOf<String, MutableSet<String>>()
    private val classMap = mutableMapOf<String, Set<String>>()
    private val maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate = 2

    init {
        generateEntitySets()
        generateDistances()
    }

    private fun generateEntitySets() {
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.parsing.indicator")
        for ((i, ktClassPointer) in project.classes.withIndex()) {
            indicator.fraction = i.toDouble() / (2 * project.classes.size)
            val ktClass = ktClassPointer.element ?: continue
            val classSignature = (ktClass as? KtClassOrObject)?.signature ?: continue
            if (!(ktClass is KtClass && ktClass.isEnum())) {
                val classEntity = ClassEntity(ktClass, classSignature)
                classes[classEntity.signature] = classEntity
            }
        }
        indicator.fraction = 0.5
        for ((i, myClass) in classes.values.withIndex()) {
            indicator.fraction = 0.5 + i.toDouble() / (2 * classes.size)
            val classEntitySet: MutableSet<String> = mutableSetOf()
            for (method in myClass.methodList) {
                val methodSignature = method.signature ?: continue
                if (methodIsDelegate(method) == null && method.signature !in entityMap) {
                    entityMap[methodSignature] = handleReferencesInMethodBody(method, methodSignature)
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

    private fun handleReferencesInMethodBody(method: KtNamedFunction, methodSignature: String): MutableSet<String> {
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

    private fun generateDistances() {
        val entitySignatures = arrayOfNulls<String>(entityList.size)
        val classSignatures = arrayOfNulls<String>(classList.size)

        for ((i, entity) in entityList.withIndex()) {
            val entityName = entity.signature ?: continue
            entitySignatures[i] = entityName
            entityIndexMap[entityName] = i
        }
        for ((j, myClass) in classList.withIndex()) {
            val className = myClass.signature
            classSignatures[j] = className
            if (!classIndexMap.containsKey(className))
                classIndexMap[className] = j
        }
    }

    fun getMoveMethodCandidateRefactoringsByAccess(classNamesToBeExamined: Set<String>, indicator: ProgressIndicator): List<MoveMethodCandidateRefactoring> {
        val candidateRefactoringList: MutableList<MoveMethodCandidateRefactoring> = mutableListOf()
        indicator.text = KotlinCodeSmellDetectorBundle.message("feature.envy.identification.indicator")
        for ((i, entity) in entityList.withIndex()) {
            indicator.fraction = i.toDouble() / entityList.size
            if (entity is KtNamedFunction) {
                val sourceClass: String = entity.containingClassOrObject?.signature ?: continue
                if (classNamesToBeExamined.contains(sourceClass)) {
                    val entitySet = entityMap[entity.signature]!!
                    val accessMap = computeAccessMap(entitySet)
                    val sortedByAccessMap = TreeMap<Int, MutableList<String>>()
                    for (targetClass in accessMap.keys) {
                        val numberOfAccessedEntities = accessMap[targetClass]?.size ?: 0
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
                                val methodEntitySet = entityMap[entity.signature]!!
                                val sourceClassEntitySet = classMap[sourceClass]!!
                                val targetClassEntitySet = classMap[targetClass]!!
                                val intersectionWithSourceClass: MutableSet<String> = methodEntitySet.intersect(sourceClassEntitySet).toMutableSet()
                                val intersectionWithTargetClass: MutableSet<String> = methodEntitySet.intersect(targetClassEntitySet).toMutableSet()
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
        }
        indicator.fraction = 1.0
        return candidateRefactoringList
    }

    private fun targetClassInheritedByAnotherCandidateTargetClass(targetClass: String, candidateTargetClasses: Set<String>): Boolean {
        // TODO: not yet implemented
        return false
    }

    private fun computeAccessMap(entitySet: Set<String>): Map<String, ArrayList<String>> {
        val accessMap: MutableMap<String, ArrayList<String>> = mutableMapOf()
        for (entity in entitySet) {
            val classOrigin = entity.substringBeforeLast("(").substringBeforeLast(".")
            if (accessMap.containsKey(classOrigin)) {
                val list = accessMap[classOrigin]!!
                list.add(entity)
            } else {
                val list = ArrayList<String>()
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