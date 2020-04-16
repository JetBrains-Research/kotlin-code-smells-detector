package testdata.core.distance.godclass.MethodProperties

class TestContainsSuperMethodInvocation : BaseClass() {
    private val a = 1
    private val b = 1
    private val c = 1
    private val d = 1
    private val e = 1

    fun fun1() {
        print(a)
        print(b)
        print(c)

        baseFun()
    }

    fun fun2() {
        print(d)
        print(e)

        super.baseFun()
    }
}

open class BaseClass {
    fun baseFun() {

    }
}