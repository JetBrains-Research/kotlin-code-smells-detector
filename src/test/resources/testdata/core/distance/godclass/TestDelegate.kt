package testdata.core.distance.godclass.MethodProperties

class TestDelegate {
    private val a = 1
    private val b = 1
    private val c = 1
    private val d = 1
    private val e = 1

    fun fun0() {
        fun1(a, b, c)
    }

    fun fun1(a: Int, b : Int, c : Int) {
        print(a)
        print(b)
        print(c)
    }

    @Synchronized
    fun fun2() {
        print(d)
        print(e)
    }
}