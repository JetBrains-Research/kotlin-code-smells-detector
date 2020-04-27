package testdata.core.distance.featureenvy

open class Base {
    fun f1() {}
    fun f2() {}
}
class Derived: Base() {
    fun f3() {}
}

class TargetClassIsInheritedByAnotherTargetClass {
    fun testMethodBase(base: Base, derived: Derived) {
        base.f1()
        base.f2()
        derived.f1()
        derived.f2()
    }
    fun testMethodDerived(base: Base, derived: Derived) {
        base.f1()
        base.f2()
        derived.f3()
    }
}