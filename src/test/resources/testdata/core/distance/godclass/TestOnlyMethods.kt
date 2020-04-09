package ru.hse.godclass

class testOnlyMethods {
    fun fun1() {
        fun2()
        fun3()
    }

    fun fun2() {
        fun1()
        fun3()
    }

    fun fun3() {
        fun1()
        fun2()
    }

    fun fun4() {
        fun5()
        fun6()
    }

    fun fun5() {
        fun4()
    }

    fun fun6() {
        fun5()
        fun4()
    }
}