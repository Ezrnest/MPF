package test

import cn.ancono.mpf.builder.buildFormula


/*
 * Created by liyicheng at 2020-04-10 19:28
 */
object BuilderTest {
    fun test1() {
        val f = buildFormula {
            forAny(x) {
                (x belongTo A) implies (x belongTo B)
            } equivTo "contains".p(A, B)
        }
        println(f)
    }
}

fun main() {
    BuilderTest.test1()
}