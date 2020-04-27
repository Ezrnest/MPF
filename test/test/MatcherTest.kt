package test

import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.matcher.buildMatcher


/*
 * Created by liyicheng at 2020-04-25 19:24
 */
object MatcherTest {
    fun test1() {
        val f = buildFormula {
            forAny(x) {
                x belongTo A implies (x belongTo B)
            } equivTo "contains".p(A, B)
        }
        println(f)

        val matcher = buildMatcher {
            forAny(x) {
                x belongTo A implies (x belongTo B)
            } equivTo "contains".r(A, B)
        }

        println(matcher.match(f))
    }

    fun test2() {
        val f = buildFormula {
            "contains".p(A, B) and "contains".p(B, A)
        }
        println(f)

        val matcher = buildMatcher {
            "contains".r(X, Y)
        }

        val replace = matcher.replaceOne(f){
            forAny(a) {
                (a belongTo X) implies (a belongTo Y)
            }
        }
        replace.forEach { println(it) }
        val replace2 = matcher.replaceAll(f){
            forAny(a) {
                (a belongTo X) implies (a belongTo Y)
            }
        }
        println(replace2)
    }

    fun test3() {
        val f = buildFormula {
            val P1 = +"P1"
            val P2 = +"P2"
            val P3 = +"P3"
            val P4 = +"P4"
            ((P1 equivTo P2) and (!P3) and R and P4) implies P1
        }
        println(f)

        val matcher = buildMatcher {
            val S = "S".ref
            ((R and (P equivTo Q)) with S) implies P

        }
        val result = matcher.match(f)
        println(result)

    }

}

fun main() {
    MatcherTest.test3()
}