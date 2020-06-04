package testdata.longmethod

fun test1(): Int {
    var a = 1
    a += 2
    a += 2
    return a
}

class Clazz {
    fun test2(): Int {
        var a = 1
        a += 2
        a += 2
        return a
    }

    companion object {
        fun test5(): Int {
            var a = 1
            a += 2
            a += 2
            return a
        }
    }

    class Inner {
        fun test3(): Int {
            var a = 1
            a += 2
            a += 2
            return a
        }

        class InnerInner {
            fun test4(): Int {
                var a = 1
                a += 2
                a += 2
                return a
            }
        }
    }
}