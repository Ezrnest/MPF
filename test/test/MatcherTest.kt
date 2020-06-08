package test

import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.matcher.VarRefFormulaMatcher
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

        val replace = matcher.replaceOne(f) {
            forAny(a) {
                (a belongTo X) implies (a belongTo Y)
            }
        }
        replace.forEach { println(it) }
        val replace2 = matcher.replaceAll(f) {
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
        result.forEach { println(it) }

    }

    fun test4() {
        val f = buildFormula {
            forAny(x) { x equalTo x } and forAny(y) { y equalTo y }
        }
        val matcher = buildMatcher {
            forAny(x) { phi(x) } and forAny(x) { phi(x) }
        }

        val re = matcher.match(f)
        re.forEach { println(it) }
    }


    fun test5() {
        val f = buildFormula {
            forAny(x) { x equalTo x } and (a equalTo a)
        }
        val matcher = buildMatcher {
            forAny(x) { phi(x) } and phi(y)
        }

        val re = matcher.match(f)
        re.forEach { println(it) }
    }


    fun test6() {
        val f = buildFormula {
            forAny(a) { exist(b) {
                (a equalTo b) implies (a equalTo b)
            } } and (x equalTo x)
        }
        println(f)
        val matcher = buildMatcher {
            forAny(x){ exist(y){
                phi(x,y) implies (x equalTo y)
            } } and psi(a)
        }

        val re = matcher.match(f)
        println("Results:")
        re.forEach { println(it) }
    }


}

fun main() {
    MatcherTest.test6()
}