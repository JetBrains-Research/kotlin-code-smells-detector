package ru.hse.godclass

class testSeparateBlocks {
    private var a = 0
    private var b = 0
    private var c = 0
    private var d = 0
    private var e = 0
    fun fun1() {
        synchronized(this) {
            a += 1
            b += 1
            c += 1
        }
    }

    fun fun2() {
        synchronized(this) {
            d += 1
            e += 1
        }
    }
}