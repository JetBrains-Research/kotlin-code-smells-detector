package testdata.core.distance.featureenvy

class FieldAccessThroughParameterCountSource {
    fun testMethod(target: FieldAccessThroughParameterCountTarget) {
        target.firstField += "ab"
        target.secondField += "cd"
    }
}

class FieldAccessThroughParameterCountTarget(var firstField: String) {
    var secondField = ""
}