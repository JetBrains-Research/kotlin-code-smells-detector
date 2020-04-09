package ru.hse.godclass

class TestSeparateBlocks {
    private var a = 0
    private var b = 0
    private var c = 0
    private var d = 0
    private var e = 0

    @Synchronized
    fun fun1() {
        a += 1
        b += 1
        c += 1
    }

    @Synchronized
    fun fun2() {
        d += 1
        e += 1
    }
}