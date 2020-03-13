package org.jetbrains.research.kotlincodesmelldetector.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.*;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PsiUtils {

    private static final String FILE_TYPE_NAME = "Kotlin";

    public static List<SmartPsiElementPointer<KtFile>> extractFiles(Project project) {
        final List<SmartPsiElementPointer<KtFile>> ktFiles = new ArrayList<>();

        ProjectFileIndex.SERVICE.getInstance(project).iterateContent(
                file -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null && !psiFile.isDirectory()
                            && FILE_TYPE_NAME.equals(psiFile.getFileType().getName())) {
                        ktFiles.add(toPointer((KtFile) psiFile));
                    }
                    return true;
                }
        );

        return ktFiles;
    }

    public static List<SmartPsiElementPointer<KtClassOrObject>> extractClasses(@Nullable KtFile ktFile) {
        if (ktFile == null) {
            return new ArrayList<>();
        }

        return ktFile.getDeclarations()
                .stream()
                .filter(ktDeclaration -> ktDeclaration instanceof KtClassOrObject)
                .map(ktDeclaration -> toPointer((KtClassOrObject) ktDeclaration))
                .collect(Collectors.toList());
    }

    public static boolean isChild(@NotNull KtElement parent, @NotNull KtElement child) {
        if (parent.equals(child)) return false;
        return child.getTextRange().getStartOffset() >= parent.getTextRange().getStartOffset()
                && child.getTextRange().getEndOffset() <= parent.getTextRange().getEndOffset();
    }

    public static boolean isTestClass(final @NotNull KtClass ktClass) {
        KtFile file = (KtFile) ktClass.getContainingFile();
        return isInsideTestDirectory(file);
    }

    private static boolean isInsideTestDirectory(final @NotNull KtFile file) {
        Optional<PsiDirectory> optionalDirectory = getDirectoryWithRootPackageFor(file);

        if (!optionalDirectory.isPresent()) {
            return false;
        }

        PsiDirectory directory = optionalDirectory.get();

        while (directory != null) {
            String dirName = directory.getName().toLowerCase();
            if (dirName.equals("test") || dirName.equals("tests")) {
                return true;
            }

            directory = directory.getParentDirectory();
        }

        return false;
    }

    private static @NotNull
    Optional<PsiDirectory> getDirectoryWithRootPackageFor(final @NotNull KtFile file) {
        String packageName = file.getPackageFqName().asString();
        String[] packageSequence;

        if ("".equals(packageName)) {
            packageSequence = new String[0];
        } else {
            packageSequence = packageName.split("\\.");
        }

        ArrayUtils.reverse(packageSequence);

        PsiDirectory directory = file.getParent();
        if (directory == null) {
            throw new IllegalStateException("File has no parent directory");
        }

        for (String packagePart : packageSequence) {
            if (!packagePart.equals(directory.getName())) {
                return Optional.empty();
            }

            directory = directory.getParentDirectory();
            if (directory == null) {
                return Optional.empty();
            }
        }

        return Optional.of(directory);
    }

    public static String calculateSignature(KtFunction function) {
        //TODO
        return null;
    }

    public static String getHumanReadableName(@Nullable KtElement element) {
        //TODO
        return null;
    }

    public static KtFunction resolveMethod(KtCallExpression callExpression) {
        //TODO
        return null;
    }

    public static <T extends PsiElement> SmartPsiElementPointer<T> toPointer(@NotNull T psiElement) {
        return SmartPointerManager.createPointer(psiElement);
    }
}