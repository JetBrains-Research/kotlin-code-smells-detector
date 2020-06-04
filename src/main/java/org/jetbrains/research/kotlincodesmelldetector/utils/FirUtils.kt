package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.cfa.TraverseDirection
import org.jetbrains.kotlin.fir.analysis.cfa.traverse
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.getOrBuildFir

fun extractFirSimpleFunctions(firFile: FirFile): List<FirSimpleFunction> {
    val result = mutableListOf<FirSimpleFunction>()
    firFile.acceptChildren(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirSimpleFunction) {
                result.add(element)
            }
            element.acceptChildren(this)
        }
    })
    return result
}

fun getCurrentFirFileOpenInEditor(project: Project): FirFile {
    val ktFile = getCurrentFileOpenInEditor(project)
    val firFile = ktFile?.getOrBuildFir(FirModuleResolveStateImpl(FirProjectSessionProvider(project)), FirResolvePhase.BODY_RESOLVE)!!
    return firFile
}

/**
 * Returns variables inside function as well as function parameters
 */
fun FirSimpleFunction.getVariableDeclarationsAndParameters() : List<FirVariable<*>> {
    val result = mutableListOf<FirVariable<*>>()
    result.addAll(valueParameters)
    result.addAll(getVariableDeclarations())
    return result
}

/**
 * Returns variables declarations inside function
 */
fun FirSimpleFunction.getVariableDeclarations() : List<FirVariable<*>> {
    val result = mutableListOf<FirVariable<*>>()
    this.acceptChildren(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirVariable<*> && !element.name.isSpecial) {
                result.add(element)
            }
            element.acceptChildren(this)
        }
    })
    return result
}

fun CFGNode<*>.isEnterOrExitNode(): Boolean {
    return (this is EnterNodeMarker) || (this is ExitNodeMarker)
}

fun CFGNode<*>.isExitNode(): Boolean {
    return this is ExitNodeMarker
}

fun isChild(parent: FirElement, child: FirElement): Boolean {
    if (child.source == null || parent.source == null) {
        return false
    }
    return if (parent == child) false else child.source!!.startOffset >= parent.source!!.startOffset
            && child.source!!.endOffset <= parent.source!!.endOffset
}

fun isChild(parent: CFGNode<*>, child: CFGNode<*>): Boolean {
    return isChild(parent.fir, child.fir)
}

// gets the corresponding psi element if any
fun FirStatement.getPsiElement(): PsiElement? {
    return this.source.psi
}

fun ControlFlowGraph.getTraversedNodes(): MutableList<CFGNode<*>> {
    val nodes = mutableListOf<CFGNode<*>>()
    val visitor = object : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {
            nodes.add(node)
        }
    }
    this.traverse(TraverseDirection.Forward, visitor)
    return nodes
}

// TODO make different from declares
fun usesLocalVariable(firElement: FirElement, firVariable: FirVariable<*>): Boolean {
    var usesLocalVariable = false
    firElement.acceptChildren(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (firElement is FirProperty) {
                if (firElement == firVariable) {
                    usesLocalVariable = true
                }
            }
            element.acceptChildren(this)
        }
    })
    return usesLocalVariable
}

fun declaresLocalVariable(firElement: FirElement, firVariable: FirVariable<*>): Boolean {
    var declaresLocalVariable = false
    firElement.acceptChildren(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (firElement is FirProperty) {
                if (firElement == firVariable) {
                    declaresLocalVariable = true
                }
            }
            element.acceptChildren(this)
        }
    })
    return declaresLocalVariable
}

fun definesLocalVariable(firElement: FirElement, firVariable: FirVariable<*>): Boolean {
    var definesLocalVariable = false
    firElement.acceptChildren(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (firElement is FirProperty) {
                if (firElement == firVariable) {
                    definesLocalVariable = true
                }
            } else if (firElement is FirVariableAssignment) {
                if (firElement.lValue is FirNamedReference) {
                    if ((firElement.lValue as FirNamedReference).name == firVariable.name) {
                        definesLocalVariable = true
                    }
                }
                element.acceptChildren(this)
            }
        }
    })
    return definesLocalVariable
}