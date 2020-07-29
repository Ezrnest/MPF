package test

import cn.ancono.mpf.core.Variable
import cn.ancono.mpf.builder.buildFormula
import kotlin.test.assertTrue


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
        println(f.regularizeAllVarName().first)
        println(f.renameAllVar(mapOf(Variable("x") to Variable("x1"))))

    }

    fun test2() {
        val f = buildFormula {
            forAny(x) { exist(y) { x belongTo y } }
        }
        val g = buildFormula {
            forAny(y) { exist(x) { y belongTo x } }
        }
//        println(f)
//        println(g)
        assertTrue {
            f.isIdentityTo(g)
        }
    }

    fun test3() {
        val f = buildFormula {
            (a equalTo a) and forAny(a) {
                a equalTo a
            }
        }
        val expected = buildFormula {
            (b equalTo b) and forAny(a) {
                a equalTo a
            }
        }
        assertTrue {
            expected.isIdentityTo(f.renameVar(mapOf(Variable("a") to Variable("b"))))
        }
    }

    fun testRegularForm() {
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
        val g = buildFormula {
            exist(x) {
                exist(y) {
                    !(x equalTo y)
                }
            } and forAny(x) {
                exist(y) {
                    !(x equalTo y)
                }
            }
        }
        println(f)
        println(f.regularForm)
        println(g.regularForm)
    }

    fun testRegularForm2() {
        val f = buildFormula {
            forAny(x) {
                (x belongTo A) implies (x belongTo B)
            } and
            forAny(x) {
                ("$3".v belongTo B) implies ("$3".v belongTo A)
            }
        }
        val r = f.regularForm
        println(r)
        println(r.regularForm)
    }

}

fun main() {
    FormulaTest.testRegularForm2()
}