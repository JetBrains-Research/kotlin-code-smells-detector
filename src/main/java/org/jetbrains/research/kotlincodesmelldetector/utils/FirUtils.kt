package org.jetbrains.research.kotlincodesmelldetector.utils

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.analysis.cfa.TraverseDirection
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.idea.fir.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.getOrBuildFir

fun extractFirSimpleFunctions(firFile: FirFile): List<FirSimpleFunction> {
    val result = mutableListOf<FirSimpleFunction>()
    result.addAll(firFile.declarations.filterIsInstance<FirSimpleFunction>())
    // TODO add check for parser generated
    // TODO we need not only regular classes I guess, see original method
    result.addAll(firFile.declarations.filterIsInstance<FirRegularClass>().flatMap {
        it.declarations.filterIsInstance<FirSimpleFunction>()
    })
    return result
}

fun getCurrentFirFileOpenInEditor(project: Project): FirFile {
    val ktFile = getCurrentFileOpenInEditor(project)
    val transformer = FirTotalResolveTransformer()
    val firFile = ktFile?.getOrBuildFir(FirModuleResolveStateImpl(FirProjectSessionProvider(project)))!!
    transformer.processFiles(listOf(firFile))
    return firFile
}

/**
 * Returns variables inside as well as function parameters
 */
fun getVariableDeclarationsInFunction(firSimpleFunction: FirSimpleFunction, cfg: ControlFlowGraph): List<FirVariable<*>> {
    // TODO meaning of asterisk here
    val result = mutableListOf<FirVariable<*>>()
    result.addAll(firSimpleFunction.valueParameters)
    // TODO any resolve needed?

    result.addAll(cfg.nodes.filterIsInstance<VariableDeclarationNode>().map { it.fir })
    return result
}

/**
 * Returns variables inside as well as function parameters
 */
fun getVariableDeclarationsInFunction(firSimpleFunction: FirSimpleFunction): List<FirVariable<*>> {
    // TODO meaning of asterisk here
    val result = mutableListOf<FirVariable<*>>()
    result.addAll(firSimpleFunction.valueParameters)
    // TODO any resolve needed?

    if (firSimpleFunction.controlFlowGraphReference !is FirControlFlowGraphReferenceImpl) {
        // TODO
        return emptyList()
    }
    val cfg = (firSimpleFunction.controlFlowGraphReference as FirControlFlowGraphReferenceImpl).controlFlowGraph
    result.addAll(cfg.nodes.filterIsInstance<VariableDeclarationNode>().map { it.fir })
    return result
}