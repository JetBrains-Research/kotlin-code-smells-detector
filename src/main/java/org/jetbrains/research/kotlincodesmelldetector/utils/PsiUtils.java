package org.jetbrains.research.kotlincodesmelldetector.utils;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.fir.FirElement;

public class PsiUtils {
    public static boolean isChild(@NotNull FirElement parent, @NotNull FirElement child) {
        if (parent.equals(child)) {
            return false;
        }
        return child.getSource().getStartOffset() >= parent.getSource().getStartOffset()
                && child.getSource().getEndOffset() <= parent.getSource().getEndOffset();
    }

    public static String getHumanReadableName(@Nullable PsiElement element) {
        if (element instanceof PsiMethod) {
            return calculateSignature((PsiMethod) element);
        } else if (element instanceof PsiClass) {
            if (element instanceof PsiAnonymousClass) {
                return getHumanReadableName(((PsiAnonymousClass) element).getBaseClassReference().resolve());
            }
            return ((PsiClass) element).getQualifiedName();
        } else if (element instanceof PsiField) {
            final PsiMember field = (PsiMember) element;
            return getHumanReadableName(field.getContainingClass()) + "." + field.getName();
        }
        return "???";
    }

    public static String calculateSignature(PsiMethod method) {
        final PsiClass containingClass = method.getContainingClass();
        final String className;
        if (containingClass != null) {
            className = containingClass.getQualifiedName();
        } else {
            className = "";
        }
        final String methodName = method.getName();
        final StringBuilder out = new StringBuilder(50);
        out.append(className);
        out.append("::");
        out.append(methodName);
        out.append('(');
        final PsiParameterList parameterList = method.getParameterList();
        final PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i != 0) {
                out.append(',');
            }
            final PsiType parameterType = parameters[i].getType();
            final String parameterTypeText = parameterType.getPresentableText();
            out.append(parameterTypeText);
        }
        out.append(')');
        return out.toString();
    }
}
