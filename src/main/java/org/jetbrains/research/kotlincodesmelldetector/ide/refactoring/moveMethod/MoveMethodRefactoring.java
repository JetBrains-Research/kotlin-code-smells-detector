package org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.moveMethod;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.Refactoring;

import static org.jetbrains.research.kotlincodesmelldetector.core.distance.DistanceMatrixKt.getNameWithParameterList;

import java.util.Optional;


/**
 * Representation of a refactoring, which moves method to a target class.
 * Once the method moved, the corresponding pointer becomes invalid.
 */
public class MoveMethodRefactoring implements Refactoring {
    private final @NotNull
    SmartPsiElementPointer<KtNamedFunction> method;
    private final @NotNull
    SmartPsiElementPointer<KtClassOrObject> targetClass;
    private final @NotNull
    String qualifiedMethodName;
    private final int sourceAccessedMembers;
    private final int targetAccessedMembers;

    /**
     * Creates refactoring.
     *
     * @param method      a method that is moved in this refactoring.
     * @param targetClass destination class in which given method is placed in this refactoring.
     */
    public MoveMethodRefactoring(
            final @NotNull KtNamedFunction method,
            final @NotNull KtClassOrObject targetClass,
            int sourceAccessedMembers,
            int targetAccessedMembers
    ) {
        this.method = ApplicationManager.getApplication().runReadAction(
                (Computable<SmartPsiElementPointer<KtNamedFunction>>) () ->
                        SmartPointerManager.getInstance(method.getProject()).createSmartPsiElementPointer(method)

        );
        this.targetClass = ApplicationManager.getApplication().runReadAction(
                (Computable<SmartPsiElementPointer<KtClassOrObject>>) () ->
                        SmartPointerManager.getInstance(targetClass.getProject()).createSmartPsiElementPointer(targetClass)
        );
        this.qualifiedMethodName = getNameWithParameterList(this.method.getElement()).toString();
        this.sourceAccessedMembers = sourceAccessedMembers;
        this.targetAccessedMembers = targetAccessedMembers;
    }

    /**
     * Returns method that is moved in this refactoring.
     */
    public @NotNull
    Optional<KtNamedFunction> getOptionalMethod() {
        return Optional.ofNullable(method.getElement());
    }

    /**
     * Returns class in which method is placed in this refactoring.
     */
    public @NotNull
    Optional<KtClassOrObject> getOptionalTargetClass() {
        return Optional.ofNullable(targetClass.getElement());
    }

    @NotNull
    @Override
    public String getDescription() {
        return getNameWithParameterList(method.getElement()).toString() + DELIMITER
                + targetClass.getElement().getFqName().toString();
    }

    @NotNull
    @Override
    public String getExportDefaultFilename() {
        return "Feature-Envy";
    }

    @NotNull
    public String getQualifiedMethodName() {
        return qualifiedMethodName;
    }

    public int getSourceAccessedMembers() {
        return sourceAccessedMembers;
    }

    public int getTargetAccessedMembers() {
        return targetAccessedMembers;
    }
}
