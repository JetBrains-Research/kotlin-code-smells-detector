package ru.hse.godclass

class TestEnclosingAccess {
    private var outerField = 0

    inner class InnerClass {
        private var a = 0
        private var b = 0
        private var c = 0
        private var d = 0
        private var e = 0
        fun fun1() {
            a += 1
            b += 1
            c += 1
            outerField += 1
        }

        fun fun2() {
            d += 1
            e += 1
            outerField += 1
        }
    }
}