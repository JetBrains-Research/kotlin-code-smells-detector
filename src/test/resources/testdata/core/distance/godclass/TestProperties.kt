class TestProperties {
    private val a = 1
    private val b = 1
    private val c = 1
    private val d = 1
    private val e = 1

    private val property1: Unit
        get() {
            print(a)
            print(b)
            print(c)
        }

    private var property2: Unit
        get() {
            print(d)
        }
        set(value) {
            print(e)
            print(value)
        }
}