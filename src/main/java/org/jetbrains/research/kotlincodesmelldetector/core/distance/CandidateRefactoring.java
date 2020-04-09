package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtElement;

public abstract class CandidateRefactoring {
    public abstract SmartPsiElementPointer<KtElement> getSourceEntity();

    public abstract SmartPsiElementPointer<KtElement> getSource();

    public abstract SmartPsiElementPointer<KtClassOrObject> getTarget();

    public abstract int getDistinctSourceDependencies();

    public abstract int getDistinctTargetDependencies();

}
