package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateGroup;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractClassCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.core.distance.ExtractedConcept;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.RefactoringType.AbstractCandidateRefactoringGroup;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoring;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassCandidateRefactoringGroup;

import java.util.ArrayList;
import java.util.List;

public class GodClassTreeTableModel extends AbstractTreeTableModel {
    public GodClassTreeTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames) {
        super(candidateRefactoringGroups, columnNames, new ExtractClassRefactoringType());
    }

    @Override
    public Object getValueAt(Object o, int index) {
        if (o instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractExtractClassCandidateRefactoringGroup =
                    (AbstractExtractClassCandidateRefactoringGroup) o;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractExtractClassCandidateRefactoringGroup.getCandidateRefactoringGroup();

            if (index == 0) {
                return group.getSource().getElement().getName();
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
                    return candidateRefactoring.getSourceEntity().getElement().getName();
                case 2:
                    return candidateRefactoring.getExtractedFields().size() + "/" + candidateRefactoring.getExtractedFunctions().size();
            }
        }

        return "";
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractGroup = (AbstractExtractClassCandidateRefactoringGroup) parent;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractGroup.getCandidateRefactoringGroup();

            return group.getExtractedConcepts().size();
        } else if (parent instanceof ExtractedConceptAndChildren) {
            ExtractedConceptAndChildren concept = (ExtractedConceptAndChildren) parent;
            return concept.children.size();
        } else {
            return super.getChildCount(parent);
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof AbstractExtractClassCandidateRefactoringGroup) {
            AbstractExtractClassCandidateRefactoringGroup abstractGroup = (AbstractExtractClassCandidateRefactoringGroup) parent;
            ExtractClassCandidateGroup group = (ExtractClassCandidateGroup) abstractGroup.getCandidateRefactoringGroup();

            return new ExtractedConceptAndChildren(group.getExtractedConcepts().get(index));
        } else if (parent instanceof ExtractedConceptAndChildren) {
            ExtractedConceptAndChildren concept = (ExtractedConceptAndChildren) parent;
            return concept.children.get(index);
        } else {
            return super.getChild(parent, index);
        }
    }

    private static class ExtractedConceptAndChildren {
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
            return extractedConcept.toString();
        }

        @Override
        public int hashCode() {
            return extractedConcept.hashCode();
        }
    }
}