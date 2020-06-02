package org.jetbrains.research.kotlincodesmelldetector.core.longmethod;

import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch;

class PDGDataDependence extends PDGAbstractDataDependence {

    PDGDataDependence(PDGNode src, PDGNode dst,
                      FirVariable<?> data, FirWhenBranch loop) {
        super(src, dst, PDGDependenceType.DATA, data, loop);
    }

}