package test

import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.core.*
import cn.ancono.mpf.structure.ZFC
import test.RuleTest.testLogic
import test.RuleTest.testLogic2
import test.RuleTest.testLogic3
import test.RuleTest.testLogic4
import kotlin.test.assertTrue


/*
 * Created by liyicheng at 2020-06-07 18:53
 */
object RuleTest {
    val rule = LogicRules.AllLogicRule

    fun applyRule(r : Rule, f : Formula, g : Formula): TowardResult {
        return r.applyToward(FormulaContext(listOf(f)), emptyList(), emptyList(),g)
    }

    fun testLogic(){

        val f1 = buildFormula { !!P }
        val f2 = buildFormula { Q }
        val g = buildFormula { P and Q }
        val re = rule.applyToward(FormulaContext(listOf(f1,f2)), emptyList(), emptyList(),g)
        assertTrue { re is Reached }
    }

    fun testLogic2(){
        val f = ZFC.buildFormula {
            (a equalTo b) and forAny(x){
                !(x belongTo a)
            }
        }
        val g = ZFC.buildFormula {
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

    fun testLogic4(){
        val f = ZFC.buildFormula {
            forAny(x){
                (x belongTo A) implies (x belongTo B)
            } and
            forAny(x){
                (x belongTo B) implies (x belongTo A)
            }
        }
        println("f: $f")
//        println(f.flatten())
        val g = ZFC.buildFormula {
            forAny(x){
                ((x belongTo A) implies (x belongTo B)) and
                        ((x belongTo B) implies (x belongTo A))
            }
        }
        val h =ZFC.buildFormula {
            forAny(x){
                (x belongTo A) equivTo (x belongTo B)
            }
        }
        println("h: $h")
//        val rec = LogicRules.RuleForAnyAnd
//        var re = applyRule(rec,f,g)
//        println(re)
//        re = applyRule(LogicRules.RuleDefEquivTo,g,h)
//        println(re)
        val re = applyRule(rule,f,h)
        assertTrue { re is Reached }
        if (re is Reached) {
            val de = re.result
            val tree = de.moreInfo["DeductionTree"]
            if (tree is DeductionNode) {
                println("Deduction Tree from f to h:")
                println(tree.toStringAsTree())
            }
        }
    }

    fun testMatcherEquiv(){
        val rule = ZFC.RuleExtension
        val f = ZFC.buildFormula {
            forAny(x) {
                (x belongTo A) implies (x belongTo B)
            } and forAny(x) {
                (x belongTo B) implies (x belongTo A)
            }
        }
        println(f)
        val g = ZFC.buildFormula {
            TODO()
        }
        rule.applyToward(FormulaContext(listOf(f)), emptyList(), emptyList(),g)
    }
}

fun main() {
//    testLogic()
//    testLogic2()
//    testLogic3()
    testLogic4()
}