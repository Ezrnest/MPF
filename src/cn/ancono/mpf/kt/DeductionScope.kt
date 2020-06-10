package cn.ancono.mpf.kt

import cn.ancono.mpf.builder.SimpleFormulaContext
import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.core.Formula
import cn.ancono.mpf.core.QualifiedName
import cn.ancono.mpf.core.RuleHints
import cn.ancono.mpf.core.System




/*
 * Created by liyicheng at 2020-06-10 19:34
 */
typealias FormulaBuilder = SimpleFormulaContext.() -> Formula

/**
 * @author liyicheng
 */
class DeductionScope(val system: System) {

    fun de(name: String = "", fb: FormulaBuilder) : Formula {
        val f = buildFormula(fb)
        val d = system.deduce(f, RuleHints(QualifiedName(name)))
        if (d == null) {
            throw FailedToDeduceException("Cannot deduce $f by $name")
        }
        println("Get: $d")
        return d.result.f
    }

    fun assume(f : Formula, action : DeductionScope.() -> Unit){
        println("Assume: $f")
        system.addFormula(f)
        action(this)
    }

}