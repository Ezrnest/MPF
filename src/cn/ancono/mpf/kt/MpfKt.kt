package cn.ancono.mpf.kt

import cn.ancono.mpf.builder.FormulaBuilderScope
import cn.ancono.mpf.builder.SimpleFormulaScope
import cn.ancono.mpf.builder.SimpleTermScope
import cn.ancono.mpf.builder.TermBuilderScope
import cn.ancono.mpf.core.Structure
import cn.ancono.mpf.core.System
import cn.ancono.mpf.structure.LogicStructure
import cn.ancono.mpf.structure.ZFC
import cn.ancono.mpf.structure.ZFCFormulaScope
import cn.ancono.mpf.structure.ZFCTermScope


/*
 * Created by liyicheng at 2020-06-10 20:17
 */
object MpfKt {

    fun <T : TermBuilderScope, R : FormulaBuilderScope<T>>
            mpf(structure: Structure,
                r : R,
                action: DeductionScope<T, R>.() -> Unit) {
        val system = System(structure)
        val deduction = DeductionScope(system,r)
        action(deduction)
    }

    fun mpfLogic(action: DeductionScope<SimpleTermScope, SimpleFormulaScope>.() -> Unit) {
        mpf(LogicStructure,SimpleFormulaScope,action)
    }

    fun mpfZFC(action: DeductionScope<ZFCTermScope, ZFCFormulaScope>.() -> Unit) {
        mpf(ZFC,ZFCFormulaScope,action)
    }
}