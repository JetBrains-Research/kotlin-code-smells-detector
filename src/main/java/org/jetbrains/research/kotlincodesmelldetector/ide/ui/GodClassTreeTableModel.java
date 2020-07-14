package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateGroup;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractedConcept;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.RefactoringType.AbstractCandidateRefactoringGroup;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup;
import org.jetbrains.research.kotlincodesmelldetector.utils.KtUtilsKt;

import java.util.ArrayList;
import java.util.List;

public class GodClassTreeTableModel extends AbstractTreeTableModel {
    private boolean debugMode = true;

    private int numberOfGroups;
    private int numberOfClasses;

    public GodClassTreeTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames) {
        super(candidateRefactoringGroups, columnNames, new ExtractClassRefactoringType());
    }

    public GodClassTreeTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames, boolean debugMode) {
        super(candidateRefactoringGroups, columnNames, new ExtractClassRefactoringType());
        this.debugMode = debugMode;
    }

    @Override
    public Object getValueAt(Object o, int index) {
        if (o instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractExtractClassCandidateRefactoringGroup =
                    (AbstractExtractClassCandidateRefactoringGroup) o;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractExtractClassCandidateRefactoringGroup.getCandidateRefactoringGroup();

            if (index == 0) {
                return group.getSource().getElement().getName() + " || " + numberOfGroups + " \\ " + numberOfClasses;
            } else {
                return "";
            }
        } else if (o instanceof AbstractExtractClassCandidateRefactoring) {
            AbstractExtractClassCandidateRefactoring abstractCandidateRefactoring = (AbstractExtractClassCandidateRefactoring) o;
            ExtractClassCandidateRefactoring candidateRefactoring = (ExtractClassCandidateRefactoring) abstractCandidateRefactoring.getCandidateRefactoring();

            switch (index) {
                case 0:
                    return "";
                case 1:
                    if (!debugMode) {
                        return candidateRefactoring.getSourceEntity().getElement().getName();
                    } else {
                        return ""
                                + candidateRefactoring.getDistinctSourceDependencies()
                                + " \\ "
                                + candidateRefactoring.getDistinctTargetDependencies()
                                + " || "
                                + candidateRefactoring.getPseudoDistinctSourceDependencies()
                                + " \\ "
                                + candidateRefactoring.getPseudoDistinctTargetDependencies();
                    }
                case 2:
                    KtElement element = candidateRefactoring.getSourceClass().getElement();
                    return
                            candidateRefactoring.getExtractedFields().size()
                                    + " : "
                                    + KtUtilsKt.getFields(element).size()
                                    + " || "
                                    + candidateRefactoring.getExtractedMethods().size()
                                    + " : "
                                    + KtUtilsKt.getMethods(element).size();
            }
        }

        return "";
    }

    @Override
    public List<?> getChildren(Object parent) {
        if (parent instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractGroup = (AbstractExtractClassCandidateRefactoringGroup) parent;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractGroup.getCandidateRefactoringGroup();

            return group.getExtractedConcepts();
        } else if (parent instanceof ExtractedConceptAndChildren) {
            ExtractedConceptAndChildren concept = (ExtractedConceptAndChildren) parent;
            return concept.children;
        } else {
            List<?> result = super.getChildren(parent);

            numberOfGroups = result.size();
            numberOfClasses = 0;
            for (Object group : result) {
                numberOfClasses += ((AbstractCandidateRefactoringGroup) group).getCandidates().size();
            }

            return result;
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object child = super.getChild(parent, index);

        if (parent instanceof AbstractExtractClassCandidateRefactoringGroup
                && child instanceof ExtractedConcept) {
            return new ExtractedConceptAndChildren((ExtractedConcept) child);
        } else {
            return child;
        }
    }

    private class ExtractedConceptAndChildren {
        private final ExtractedConcept extractedConcept;
        private final List<AbstractExtractClassCandidateRefactoring> children;

        private ExtractedConceptAndChildren(ExtractedConcept extractedConcept) {
            this.extractedConcept = extractedConcept;
            ExtractClassCandidateRefactoring[] refactorings = extractedConcept.getConceptClusters().toArray(new ExtractClassCandidateRefactoring[0]);
            children = new ArrayList<>();
            for (ExtractClassCandidateRefactoring refactoring : refactorings) {
                children.add(new AbstractExtractClassCandidateRefactoring(refactoring));
            }
        }

        @Override
        public String toString() {
            return extractedConcept.toString() + " || " + numberOfGroups + " \\ " + numberOfClasses;
        }

        @Override
        public int hashCode() {
            return extractedConcept.hashCode();
        }
    }
}