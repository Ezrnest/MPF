package cn.ancono.mpf.sample

import cn.ancono.mpf.builder.SimpleFormulaScope
import cn.ancono.mpf.builder.SimpleTermScope
import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.kt.DeductionScope
import cn.ancono.mpf.kt.MpfKt
import cn.ancono.mpf.structure.LogicStructure
import cn.ancono.mpf.structure.ZFCFormulaScope
import cn.ancono.mpf.structure.ZFCTermScope

/*
 * Created by liyicheng at 2020-06-10 20:38
 */
object Samples {

    /**
     * An example of contradictory, [LogicStructure] should be used.
     */
    fun deduction1(): DeductionScope<SimpleTermScope, SimpleFormulaScope>.() -> Unit = {
        val f = buildFormula { P implies !P }
        assume(f) {
            // 'de' for deduction
            de("logic.DefImply") { !P or !P } // P->Q <=> !P | Q
            de { !P }
            yield() { !P }
            // we get !P from (P implies !P )
        }
        showObtained()
    }

    /**
     *
     */
    fun deduction2(): DeductionScope<ZFCTermScope, ZFCFormulaScope>.() -> Unit = {
        define("DefContains") {
            A contains B // A contains B
        } with {
            forAny(x) {
                (x belongTo B) implies (x belongTo A)
            }
        }
        val f = buildFormula {
            (A contains B) and (B contains A)
        }
        val g = buildFormula {
            A equalTo B
        }
        assume(f) {
            de("DefContains") {
                forAny(x) {
                    (x belongTo B) implies (x belongTo A)
                } and forAny(x) {
                    (x belongTo A) implies (x belongTo B)
                }
            }
            de {
                forAny(x) {
                    ((x belongTo A) implies (x belongTo B)) and ((x belongTo B) implies (x belongTo A))
                }
            }
            de {
                forAny(x) {
                    (x belongTo A) equivTo (x belongTo B)
                }
            }
            de("zfc.RuleExtension") {
                A equalTo B
            }
            yield() {
                A equalTo B
            }
        }
        showObtained()
        assume(g) {
            de("zfc.RuleExtension") {
                forAny(x) {
                    (x belongTo A) equivTo (x belongTo B)
                }
            }
            de {
                forAny(x) {
                    (x belongTo B) implies (x belongTo A)
                } and forAny(x) {
                    (x belongTo A) implies (x belongTo B)
                }
            }
            de("DefContains") {
                f
            }
            yield() { f }
        }
        showObtained()
        de {
            (f implies g) and (g implies f)
        }
        de {
            f equivTo g
        }

    }

}

fun main() {
//    MpfKt.mpfLogic(Samples.deduction1())
    MpfKt.mpfZFC(Samples.deduction2())

}