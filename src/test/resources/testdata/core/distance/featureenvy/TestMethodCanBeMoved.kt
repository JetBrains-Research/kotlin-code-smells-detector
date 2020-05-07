package testdata.core.distance.featureenvy

open class MethodCanBeMovedSource(private val target: MethodCanBeMovedTarget) {
    @Synchronized
    fun synchronizedMethod() {
        if (target.firstField + target.secondField > 10) {
            target.f1()
            target.f2()
        }
    }

    open fun openMethod() {
        if (target.firstField + target.secondField > 10) {
            target.f1()
            target.f2()
        }
    }

    fun methodWithoutValidReferenceToTargetClass() {
        val localTarget = MethodCanBeMovedTarget(1, 2)
        if (localTarget.firstField + localTarget.secondField > 10) {
            localTarget.f1()
            localTarget.f2()
        }
    }

    fun methodWithNullableReferenceToTargetClass(target: MethodCanBeMovedNullableTarget?) {
        target?.f1()
        target?.f2()
    }

    fun methodAccessesOnlyMembersOfCompanion() {
        MethodCanBeMovedTarget.f3()
        MethodCanBeMovedTarget.f4()
    }

    fun methodAccessesDataClassWithMethod(dataClass: DataClassWithMethod) {
        dataClass.firstField += dataClass.secondField
    }

    fun methodAccessesDataClassWithoutMethod(dataClass: DataClassWithoutMethod) {
        dataClass.firstField += dataClass.secondField
    }
}

class MethodCanBeMovedTarget(val firstField: Int, val secondField: Int) {
    companion object {
        fun f3() {}
        fun f4() {}
    }

    fun f1() {}
    fun f2() {}
}

class MethodCanBeMovedNullableTarget(val firstField: Int, val secondField: Int) {
    fun f1() {}
    fun f2() {}
}

data class DataClassWithMethod(var firstField: Int, var secondField: Int) {
    fun f() {}
}

data class DataClassWithoutMethod(var firstField: Int, var secondField: Int)