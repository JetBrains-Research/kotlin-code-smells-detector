package testdata.core.distance.featureenvy

import java.util.*

class SourceMethodInvocationsCountSource {
    fun testMethod(target: SourceMethodInvocationsCountTarget) {
        if (sourceMethod() > 100) {
            target.firstField = Date().time
            target.emptyMethod()
        }
    }
    private fun sourceMethod(): Int = 99
}

class SourceMethodInvocationsCountTarget(var firstField: Long) {
    fun emptyMethod() {}
}