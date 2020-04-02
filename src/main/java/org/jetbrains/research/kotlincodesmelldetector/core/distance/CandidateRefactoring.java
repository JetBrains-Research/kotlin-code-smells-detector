package org.jetbrains.research.kotlincodesmelldetector.core.distance;

import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.psi.KtClassOrObject;

public abstract class CandidateRefactoring {
    public abstract SmartPsiElementPointer<KtClassOrObject> getSourceEntity();

    public abstract SmartPsiElementPointer<KtClassOrObject> getSource();

    public abstract SmartPsiElementPointer<KtClassOrObject> getTarget();

    public abstract int getDistinctSourceDependencies();

    public abstract int getDistinctTargetDependencies();

}
