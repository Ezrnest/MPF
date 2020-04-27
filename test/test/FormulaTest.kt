package test

import cn.ancono.mpf.core.Variable
import cn.ancono.mpf.builder.buildFormula


/*
 * Created by liyicheng at 2020-04-27 20:18
 */
object FormulaTest {

    fun test1() {
        val f = buildFormula {
            forAny(x) {
                exist(y) {
                    !(x equalTo y)
                }
            } and exist(x) {
                exist(y) {
                    !(x equalTo y)
                }
            }
        }
        println(f)
        println(f.variables)
        println(f.allVariables)
        println(f.regularizeVarName())
        println(f.renameAllVar(mapOf(Variable("x") to Variable("x1"))))

    }

    fun test2() {
        val f = buildFormula {
            forAny(x) { exist(y) { x belongTo y } }
        }
        val g = buildFormula {
            forAny(y) { exist(x) { y belongTo x } }
        }
        println(f)
        println(g)
        println(f.isIdentityTo(g))
    }

}

fun main() {
    FormulaTest.test2()
}