package testdata.core.distance.featureenvy

class TargetMethodInvocationsCountSource {
    fun testMethod(target: TargetMethodInvocationsCountTarget) {
        if (target.firstMethod()) {
            target.secondMethod()
            target.thirdMethod()
        }
    }
}

class TargetMethodInvocationsCountTarget(var firstField: String, var secondField: Int) {
    var thirdField = 100
    fun firstMethod(): Boolean = true
    fun secondMethod() {}
    fun thirdMethod() {}
}
