package org.jetbrains.research.kotlincodesmelldetector;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference;
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl;
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.*;
import org.jetbrains.research.kotlincodesmelldetector.core.longmethod.*;

import java.util.*;

import static org.jetbrains.research.kotlincodesmelldetector.utils.FirUtilsKt.*;

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
    }

    public static Set<ASTSliceGroup> getExtractMethodRefactoringOpportunities(ProjectInfo project, ProgressIndicator indicator) {
        Set<ASTSliceGroup> extractedSliceGroups = new HashSet<>();
        FirFile firFile = getCurrentFirFileOpenInEditor(project.getProject());
        List<FirSimpleFunction> firSimpleFunctions = extractFirSimpleFunctions(firFile);
        for (FirSimpleFunction firSimpleFunction : firSimpleFunctions) {
            processMethod(extractedSliceGroups, firSimpleFunction);
        }
        return extractedSliceGroups;
    }

    private static void processMethod(final Set<ASTSliceGroup> extractedSliceGroups, FirSimpleFunction firSimpleFunction) {
        if (firSimpleFunction.getBody() != null) {
            if (firSimpleFunction.getControlFlowGraphReference() instanceof FirEmptyControlFlowGraphReference) {
                // TODO error handling
                throw new IllegalStateException("Empty cfg reference");
            }
            ControlFlowGraph cfg = ((FirControlFlowGraphReferenceImpl) firSimpleFunction.getControlFlowGraphReference()).getControlFlowGraph();
            for (FirVariable<?> declaration : getVariableDeclarationsInFunction(firSimpleFunction, cfg)) {
                //stringBuilder.append(declaration.getName().toString() + ' ');
                PDGSliceUnionCollection sliceUnionCollection = new PDGSliceUnionCollection(firSimpleFunction, cfg, declaration);
                //                double sumOfExtractedStatementsInGroup = 0.0;
                //                double sumOfDuplicatedStatementsInGroup = 0.0;
                //                double sumOfDuplicationRatioInGroup = 0.0;
                //                int maximumNumberOfExtractedStatementsInGroup = 0;
                //                int groupSize = sliceUnionCollection.getSliceUnions().size();
                ASTSliceGroup sliceGroup = new ASTSliceGroup();
                for (PDGSliceUnion sliceUnion : sliceUnionCollection.getSliceUnions()) {
                    ASTSlice slice = new ASTSlice(sliceUnion);
                    //   if (true) { //!slice.isVariableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement()) {
                    //                        int numberOfExtractedStatements = slice.getNumberOfSliceStatements();
                    //                        int numberOfDuplicatedStatements = slice.getNumberOfDuplicatedStatements();
                    //                        double duplicationRatio = (double) numberOfDuplicatedStatements / (double) numberOfExtractedStatements;
                    //                        sumOfExtractedStatementsInGroup += numberOfExtractedStatements;
                    //                        sumOfDuplicatedStatementsInGroup += numberOfDuplicatedStatements;
                    //                        sumOfDuplicationRatioInGroup += duplicationRatio;
                    //                        if (numberOfExtractedStatements > maximumNumberOfExtractedStatementsInGroup)
                    //                            maximumNumberOfExtractedStatementsInGroup = numberOfExtractedStatements;
                    sliceGroup.addCandidate(slice);
                }

                if (!sliceGroup.getCandidates().isEmpty()) {
                    //                    sliceGroup.setAverageNumberOfExtractedStatementsInGroup(sumOfExtractedStatementsInGroup / (double) groupSize);
                    //                    sliceGroup.setAverageNumberOfDuplicatedStatementsInGroup(sumOfDuplicatedStatementsInGroup / (double) groupSize);
                    //                    sliceGroup.setAverageDuplicationRatioInGroup(sumOfDuplicationRatioInGroup / (double) groupSize);
                    //                    sliceGroup.setMaximumNumberOfExtractedStatementsInGroup(maximumNumberOfExtractedStatementsInGroup);
                    extractedSliceGroups.add(sliceGroup);
                }
            }
            //            for (PsiVariable declaration : pdg.getVariableDeclarationsAndAccessedFieldsInMethod()) {
            //                PlainVariable variable = new PlainVariable(declaration);
            //                PDGObjectSliceUnionCollection objectSliceUnionCollection = new PDGObjectSliceUnionCollection(pdg, variable);
            //                double sumOfExtractedStatementsInGroup = 0.0;
            //                double sumOfDuplicatedStatementsInGroup = 0.0;
            //                double sumOfDuplicationRatioInGroup = 0.0;
            //                int maximumNumberOfExtractedStatementsInGroup = 0;
            //                int groupSize = objectSliceUnionCollection.getSliceUnions().size();
            //                ASTSliceGroup sliceGroup = new ASTSliceGroup();
            //                for (PDGObjectSliceUnion objectSliceUnion : objectSliceUnionCollection.getSliceUnions()) {
            //                    ASTSlice slice = new ASTSlice(objectSliceUnion);
            //                    if (!slice.isVariableCriterionDeclarationStatementIsDeeperNestedThanExtractedMethodInvocationInsertionStatement()) {
            //                        int numberOfExtractedStatements = slice.getNumberOfSliceStatements();
            //                        int numberOfDuplicatedStatements = slice.getNumberOfDuplicatedStatements();
            //                        double duplicationRatio = (double) numberOfDuplicatedStatements / (double) numberOfExtractedStatements;
            //                        sumOfExtractedStatementsInGroup += numberOfExtractedStatements;
            //                        sumOfDuplicatedStatementsInGroup += numberOfDuplicatedStatements;
            //                        sumOfDuplicationRatioInGroup += duplicationRatio;
            //                        if (numberOfExtractedStatements > maximumNumberOfExtractedStatementsInGroup)
            //                            maximumNumberOfExtractedStatementsInGroup = numberOfExtractedStatements;
            //                        sliceGroup.addCandidate(slice);
            //                    }
            //                }
            //                if (!sliceGroup.getCandidates().isEmpty()) {
            //                    sliceGroup.setAverageNumberOfExtractedStatementsInGroup(sumOfExtractedStatementsInGroup / (double) groupSize);
            //                    sliceGroup.setAverageNumberOfDuplicatedStatementsInGroup(sumOfDuplicatedStatementsInGroup / (double) groupSize);
            //                    sliceGroup.setAverageDuplicationRatioInGroup(sumOfDuplicationRatioInGroup / (double) groupSize);
            //                    sliceGroup.setMaximumNumberOfExtractedStatementsInGroup(maximumNumberOfExtractedStatementsInGroup);
            //                    extractedSliceGroups.add(sliceGroup);
            //                }
        }
    }
}