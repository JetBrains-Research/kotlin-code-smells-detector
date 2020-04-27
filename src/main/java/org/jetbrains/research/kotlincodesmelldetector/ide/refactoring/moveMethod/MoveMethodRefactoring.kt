package org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.moveMethod

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.Refactoring
import org.jetbrains.research.kotlincodesmelldetector.utils.signature
import java.util.*

/**
 * Representation of a refactoring, which moves method to a target class.
 * Once the method moved, the corresponding pointer becomes invalid.
 */
class MoveMethodRefactoring(method: KtNamedFunction, targetClass: KtClassOrObject, val sourceAccessedMembers: Int, val targetAccessedMembers: Int) : Refactoring {
    private val method: SmartPsiElementPointer<KtNamedFunction> = ApplicationManager.getApplication().runReadAction(
            Computable { SmartPointerManager.getInstance(method.project).createSmartPsiElementPointer(method) } as Computable<SmartPsiElementPointer<KtNamedFunction>>
    )
    private val targetClass: SmartPsiElementPointer<KtClassOrObject> = ApplicationManager.getApplication().runReadAction(
            Computable { SmartPointerManager.getInstance(targetClass.project).createSmartPsiElementPointer(targetClass) } as Computable<SmartPsiElementPointer<KtClassOrObject>>
    )
    val qualifiedMethodName: String = this.method.element?.signature ?: "???"

    /**
     * Returns method that is moved in this refactoring.
     */
    val optionalMethod: Optional<KtNamedFunction>
        get() = Optional.ofNullable(method.element)

    /**
     * Returns class in which method is placed in this refactoring.
     */
    val optionalTargetClass: Optional<KtClassOrObject>
        get() = Optional.ofNullable(targetClass.element)

    override fun getDescription(): String {
        return (qualifiedMethodName + Refactoring.DELIMITER + (targetClass.element?.signature ?: "???"))
    }

    override fun getExportDefaultFilename(): String {
        return "Feature-Envy"
    }
}