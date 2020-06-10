package cn.ancono.mpf.sample

import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.core.LogicStructure
import cn.ancono.mpf.kt.MpfKt


/*
 * Created by liyicheng at 2020-06-10 20:38
 */
object Samples{

    fun deduction1(){
        MpfKt.mpf(LogicStructure){
            val f = buildFormula { P implies !P }
            assume(f){
                de("logic.DefImply") { !P or !P} // P->Q <=> !P | Q
                de { !P }
            }
        }
    }


}

fun main() {
    Samples.deduction1()
}