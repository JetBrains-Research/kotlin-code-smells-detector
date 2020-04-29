package testdata.core.distance.featureenvy

class FieldAccessesCountSource(private val target: FieldAccessesCountTarget) {
    fun testMethod() {
        target.firstField = "Abc"
        if (this.target.secondField == 1000) {
            target.thirdField += 2
        }
    }
}

class FieldAccessesCountTarget(var firstField: String, var secondField: Int) {
    var thirdField = 100
}