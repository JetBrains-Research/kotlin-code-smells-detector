package org.jetbrains.research.kotlincodesmelldetector;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.*;
import org.jetbrains.research.kotlincodesmelldetector.utils.PsiUtils;

import java.util.*;

public class KotlinCodeSmellFacade {
    public static TreeSet<ExtractClassCandidateGroup> getExtractClassRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        KtFile ktFile = PsiUtils.getCurrentFileOpenInEditor(project.getProject());
        List<KtClassOrObject> ktClasses = PsiUtils.extractClasses(ktFile);

        List<ExtractClassCandidateRefactoring> extractClassCandidateList = new ArrayList<>(GodClassDistanceMatrixKt.getExtractClassCandidateRefactorings(project, ktClasses, indicator));

        HashMap<SmartPsiElementPointer<KtClassOrObject>, ExtractClassCandidateGroup> groupedBySourceClassMap = new HashMap<>();
        for (ExtractClassCandidateRefactoring candidate : extractClassCandidateList) {
            if (groupedBySourceClassMap.containsKey(candidate.getSourceEntity())) {
                groupedBySourceClassMap.get(candidate.getSourceEntity()).addCandidate(candidate);
            } else {
                ExtractClassCandidateGroup group = new ExtractClassCandidateGroup(candidate.getSourceEntity());
                group.addCandidate(candidate);
                groupedBySourceClassMap.put(candidate.getSourceEntity(), group);
            }
        }

        for (SmartPsiElementPointer<KtClassOrObject> sourceClass : groupedBySourceClassMap.keySet()) {
            groupedBySourceClassMap.get(sourceClass).groupConcepts();
        }

        return new TreeSet<>(groupedBySourceClassMap.values());
    }

    public static List<MoveMethodCandidateRefactoring> getMoveMethodRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        DistanceMatrix distanceMatrix = new DistanceMatrix(project);

        Set<FqName> classNamesToBeExamined = new LinkedHashSet<>();
        for (Map.Entry<FqName, ClassEntity> entry: distanceMatrix.getClasses().entrySet()) {
            if (!entry.getValue().isEnum() && !entry.getValue().isInterface()) {
                classNamesToBeExamined.add(entry.getKey());
            }
        }
        List<MoveMethodCandidateRefactoring> moveMethodCandidateRefactorings =
                distanceMatrix.getMoveMethodCandidateRefactoringsByAccess(classNamesToBeExamined, indicator);
        Collections.sort(moveMethodCandidateRefactorings);
        return moveMethodCandidateRefactorings;
    }
}