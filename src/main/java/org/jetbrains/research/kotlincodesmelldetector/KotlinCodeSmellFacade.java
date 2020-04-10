package org.jetbrains.research.kotlincodesmelldetector;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.*;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateGroup;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.GodClassDistanceMatrixKt;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ProjectInfo;
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.ASTSliceGroup;

import java.util.*;

import static org.jetbrains.research.kotlincodesmelldetector.utils.ExtractUtilsKt.extractClasses;
import static org.jetbrains.research.kotlincodesmelldetector.utils.ExtractUtilsKt.getCurrentFileOpenInEditor;

public class KotlinCodeSmellFacade {
    public static TreeSet<ExtractClassCandidateGroup> getExtractClassRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        List<SmartPsiElementPointer<KtElement>> classes = project.getClasses();

        List<ExtractClassCandidateRefactoring> extractClassCandidateList = new ArrayList<>(GodClassDistanceMatrixKt.getExtractClassCandidateRefactorings(project, classes, indicator));

        HashMap<SmartPsiElementPointer<KtElement>, ExtractClassCandidateGroup> groupedBySourceClassMap = new HashMap<>();
        for (ExtractClassCandidateRefactoring candidate : extractClassCandidateList) {
            if (groupedBySourceClassMap.containsKey(candidate.getSourceEntity())) {
                groupedBySourceClassMap.get(candidate.getSourceEntity()).addCandidate(candidate);
            } else {
                ExtractClassCandidateGroup group = new ExtractClassCandidateGroup(candidate.getSourceEntity());
                group.addCandidate(candidate);
                groupedBySourceClassMap.put(candidate.getSourceEntity(), group);
            }
        }

        for (SmartPsiElementPointer<KtElement> sourceClass : groupedBySourceClassMap.keySet()) {
            groupedBySourceClassMap.get(sourceClass).groupConcepts();
        }

        return new TreeSet<>(groupedBySourceClassMap.values());
    }

    public static List<MoveMethodCandidateRefactoring> getMoveMethodRefactoringOpportunities(ProjectInfo sourceProjectInfo, ProjectInfo targetProjectInfo, ProgressIndicator indicator) {
        DistanceMatrix distanceMatrix = new DistanceMatrix(targetProjectInfo, indicator);

        Set<KtClassOrObject> classesToBeExamined = new LinkedHashSet<>();
        for (SmartPsiElementPointer<KtElement> pointer : sourceProjectInfo.getClasses()) {
            KtElement element = pointer.getElement();
            if (element instanceof KtClassOrObject) {
                if (!(element instanceof KtClass && (((KtClass) element).isInterface() || ((KtClass) element).isEnum()))) {
                    classesToBeExamined.add((KtClassOrObject) element);
                }
            }
        }
        List<MoveMethodCandidateRefactoring> moveMethodCandidateRefactorings =
                distanceMatrix.getMoveMethodCandidateRefactoringsByAccess(classesToBeExamined, indicator);
        Collections.sort(moveMethodCandidateRefactorings);
        return moveMethodCandidateRefactorings;
    // TODO implement
    public static HashSet<ASTSliceGroup> getExtractMethodRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        return new HashSet<>();
    }
}