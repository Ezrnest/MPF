package test

import cn.ancono.mpf.core.buildFormula
import cn.ancono.mpf.matcher.FormulaMatcherContext
import cn.ancono.mpf.matcher.FormulaMatcherContext.belongTo
import cn.ancono.mpf.matcher.FormulaMatcherContext.implies
import cn.ancono.mpf.matcher.buildMatcher
import cn.ancono.mpf.matcher.destructed


/*
 * Created by liyicheng at 2020-04-25 19:24
 */
object MatcherTest {
    fun test1() {
        val f = buildFormula {
            forAny(x) {
                x belongTo A implies (x belongTo B)
            } equivTo "contains".r(A, B)
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
            "contains".r(A, B) and "contains".r(B, A)
        }
        println(f)

        val matcher = buildMatcher {
            "contains".r(X, Y)
        }

        val replace = matcher.replaceOne(f){
            forAny(x) {
                x belongTo X implies (x belongTo Y)
            }
        }
        println(replace)
        val replace2 = matcher.replaceAll(f){
            forAny(x) {
                x belongTo X implies (x belongTo Y)
            }
        }
        println(replace2)
    }

}

fun main() {
    MatcherTest.test2()
}