package org.jetbrains.research.kotlincodesmelldetector.core

internal interface VisualizationData {
    val distinctSourceDependencies: Int
    val distinctTargetDependencies: Int
}