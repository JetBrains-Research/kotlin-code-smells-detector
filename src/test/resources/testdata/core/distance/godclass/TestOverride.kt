class TestOverride : AnotherClass() {
    private var a = 0
    private var b = 0
    private var c = 0
    private var d = 0
    private var e = 0

    override fun methodToExtend(): String {
        a += 1
        b += 1
        c += 1
        return ""
    }

    override fun methodToExtend2(): String {
        d += 1
        e += 1
        return ""
    }
}

open class AnotherClass {
    open fun methodToExtend(): String {
        return ""
    }

    open fun methodToExtend2(): String {
        return ""
    }
}