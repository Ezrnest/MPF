package test

import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.core.FormulaContext
import cn.ancono.mpf.core.LogicRules
import cn.ancono.mpf.core.Reached
import test.RuleTest.testLogic3
import kotlin.test.assertTrue


/*
 * Created by liyicheng at 2020-06-07 18:53
 */
object RuleTest {
    val rule = LogicRules.AllLogicRule
    fun testLogic(){

        val f1 = buildFormula { !!P }
        val f2 = buildFormula { Q }
        val g = buildFormula { P and Q }
        val re = rule.applyToward(FormulaContext(listOf(f1,f2)), emptyList(), emptyList(),g)
        assertTrue { re is Reached }
    }

    fun testLogic2(){
        val f = buildFormula {
            (a equalTo b) and forAny(x){
                !(x belongTo a)
            }
        }
        val g = buildFormula {
            forAny(y){
                !(y belongTo b)
            }
        }
        val re = rule.applyToward(FormulaContext(listOf(f)), emptyList(), emptyList(),g)
        assertTrue { re is Reached }
    }

    fun testLogic3(){
        val f = buildFormula {
            P implies !P
        }
        val g = buildFormula {
            !P or !P
        }
        val def = LogicRules.RuleDefImply
        val re = def.applyToward(FormulaContext(listOf(f)), emptyList(), emptyList(),g)
        println(re)
        assertTrue { re is Reached }
    }

}

fun main() {
//    testLogic()
//    testLogic2()
    testLogic3()
}