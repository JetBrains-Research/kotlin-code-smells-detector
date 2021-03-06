package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactorJBundle;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.components.JBLabelDecorator;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.extractClass.ExtractClassRefactoring;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.regex.Pattern;

public class GodClassUserInputDialog extends RefactoringDialog {
    private static final int MAIN_PANEL_VERTICAL_GAP = 5;
    private final ExtractClassRefactoring refactoring;

    @Nullable
    private final PsiPackage parentPackage;

    private JPanel mainPanel;
    private final JTextField extractedClassNameField = new JTextField();
    private final JButton restoreButton = new JButton();

    public GodClassUserInputDialog(ExtractClassRefactoring refactoring) {
        super(refactoring.getSourceFile().getProject(), true);

        this.refactoring = refactoring;
        String packageName = refactoring.getSourceFile().getPackageFqName().asString();
        parentPackage = JavaPsiFacade.getInstance(refactoring.getProject()).findPackage(packageName);

        initialiseControls();
        setTitle(KotlinCodeSmellDetectorBundle.message("god.class.dialog.title"));
        init();
    }

    @Nullable
    @Override
    protected JComponent createNorthPanel() {
        placeControlsOnPanel();
        return mainPanel;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    private void initialiseControls() {
        extractedClassNameField.setText(refactoring.getExtractedClassName());
        extractedClassNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleInputChanged(extractedClassNameField);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleInputChanged(extractedClassNameField);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleInputChanged(extractedClassNameField);
            }
        });

        restoreButton.setText(KotlinCodeSmellDetectorBundle.message("god.class.dialog.restore.default"));

        restoreButton.addActionListener(e -> extractedClassNameField.setText(refactoring.getDefaultExtractedTypeName()));

        handleInputChanged(extractedClassNameField);
    }

    private void placeControlsOnPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder()
                .addComponent(
                        JBLabelDecorator.createJBLabelDecorator(RefactorJBundle.message("extract.class.from.label", PsiTreeUtil.findChildOfType(refactoring.getSourceFile(), PsiClass.class)))
                                .setBold(true))
                .addLabeledComponent(RefactorJBundle.message("name.for.new.class.label"), extractedClassNameField, UIUtil.LARGE_VGAP);

        JComponent emptyComponent = new JComponent() {
            @Override
            public void setInheritsPopupMenu(boolean value) {
                super.setInheritsPopupMenu(value);
            }
        };

        builder.addLabeledComponent(restoreButton, emptyComponent);

        mainPanel = builder.addVerticalGap(MAIN_PANEL_VERTICAL_GAP).getPanel();
    }

    private void setMessage(String message) {
        setErrorText(message);
    }

    private void mayApplyRefactoring(boolean value) {
        getRefactorAction().setEnabled(value);
        getPreviewAction().setEnabled(value);
    }

    private void handleInputChanged(JTextField textField) {
        String className = textField.getText();
        String classNamePattern = "[a-zA-Z_][a-zA-Z0-9_]*";
        if (!Pattern.matches(classNamePattern, className)) {
            mayApplyRefactoring(false);
            setMessage(KotlinCodeSmellDetectorBundle.message("god.class.dialog.class.name.not.valid"));
            return;
        } else if (parentPackage != null && parentPackage.containsClassNamed(className)) {
            mayApplyRefactoring(false);
            setMessage(KotlinCodeSmellDetectorBundle.message("god.class.dialog.class.name.already.exists", parentPackage.getName()));
            return;
        } else {
            refactoring.setExtractedClassName(className);
        }
        //TODO add checks on keywords / identifiers

        mayApplyRefactoring(true);
        setMessage("");
    }

    @Override
    protected void doAction() {
        closeOKAction();
        refactoring.setExtractedClassName(extractedClassNameField.getText());
        WriteCommandAction.runWriteCommandAction(refactoring.getProject(), refactoring::apply);
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }

    @Override
    protected boolean hasPreviewButton() {
        return false;
    }
}
