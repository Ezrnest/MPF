package cn.ancono.mpf.kt

import cn.ancono.mpf.builder.FormulaBuilderScope
import cn.ancono.mpf.builder.TermBuilderScope
import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.core.*


/*
 * Created by liyicheng at 2020-06-10 19:34
 */
typealias FormulaBuilder<R> = R.() -> Formula

/**
 * @author liyicheng
 */
class DeductionScope<T : TermBuilderScope, R : FormulaBuilderScope<T>>
    (val system: System,
     private val formulaBuilderScope : R
) {

    var indent : Int = 2

    fun buildFormula(fb : FormulaBuilder<R>) : Formula{
        return fb(formulaBuilderScope)
    }

    private fun printWithIndent(s: String) {
        repeat(indent * (system.contextStack.size-1)){
            print(' ')
        }
        println(s)
    }

    fun de(name: String = "", fb: FormulaBuilder<R>): Formula {
        val f = buildFormula(fb)
        val d = system.deduce(f, RuleHints(QualifiedName(name)))
            ?: throw FailedToDeduceException("Cannot deduce $f by $name")
        printWithIndent("Get: $d")
        return d.f
    }

    fun define(name: String, f: Formula, g: Formula, description: String): Rule {
        val qn = QualifiedName.parseQualified(name)
        return system.define(qn, f, g, description)
    }

    fun define(name: String, description: String = "None", fb: FormulaBuilder<R>): DefineBuilder {
        val f = buildFormula(fb)
        return DefineBuilder(name, description, f)
    }

    inner class DefineBuilder(val name: String, val description: String, val f: Formula) {
        infix fun with(gb: FormulaBuilder<R>): Rule {
            return define(name, f, buildFormula(gb), description)
        }
    }


    fun <S> assume(f: Formula, action: DeductionScope<T,R>.() -> S): S {
        printWithIndent("Assume: $f")
        system.assume(f)
        return action(this)
    }

    fun yield(fb: FormulaBuilder<R>): Formula {
        val f = buildFormula(fb)
        return system.yield(f) ?: throw FailedToDeduceException("Cannot deduce $f")
    }

    fun showObtained() {
        printWithIndent("Obtained: ${system.context.formulaContext.formulas}")
    }

}