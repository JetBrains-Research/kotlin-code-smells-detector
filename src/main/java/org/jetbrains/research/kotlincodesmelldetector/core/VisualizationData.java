package org.jetbrains.research.kotlincodesmelldetector.core;

interface VisualizationData {
    int getDistinctSourceDependencies();

    int getDistinctTargetDependencies();
}
