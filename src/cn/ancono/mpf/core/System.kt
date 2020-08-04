package cn.ancono.mpf.core

import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException


class RuleHints(val ruleName: QualifiedName) {

}

//class Deduction(val r : Rule,
//                val result: Result
//){
//    val f : Formula
//        get() = result.f
//    val moreInfo : Map<String,Any>
//        get() = result.moreInfo
//    val dependencies : List<Formula>
//        get() = result.dependencies
//
//
//}
/**
 * The system is the core of the deduction system.
 *
 * Created by liyicheng at 2020-06-09 15:43
 */
class System(val baseStructure: Structure) {
    val contextStack = arrayListOf<Context>()

    init {
        contextStack.add(Context(MutableStructure.of(baseStructure)))
    }

    /**
     * Gets the current context.
     */
    val context: Context
        get() = contextStack.last()

    private fun pushContext(context: Context) {
        contextStack.add(context)
    }

    private fun popContext(): Context {
        if (contextStack.size == 1) {
            throw UnsupportedOperationException("Cannot pop the base context!")
        }
        return contextStack.removeAt(contextStack.lastIndex)
    }

    /**
     * Create a new context extending the current with the additional assumed formulas.
     *
     * @see [yield]
     */
    fun assume(vararg fs: Formula) {
        val newContext = AssumedContext.from(fs.toList(), context)
        pushContext(newContext)
    }

    /**
     * Yield a formula based on the current context, this method
     * will automatically pop the current context if success.
     *
     * @return
     */
    fun yield(f: Formula): Formula? {
        val fs = context.formulaContext.formulas
        val ac = this.context
        if (!fs.any { it.isIdenticalTo(f) }) {
            return null
        }
        val result = if (ac is AssumedContext) {
            val assumed = ac.assumedFormulas
            val p = AndFormula(assumed)
            ImplyFormula(p, f).flatten()
        } else {
            f
        }
        popContext()
        addFormula(result)
        return result

    }

    /**
     * Adds a formula obtained to the current context.
     */
    fun addFormula(f: Formula) {
        context.formulaContext.addFormula(f)
    }

    /**
     * Adds a rule to the current context.
     */
    fun addRule(r: Rule) {
        context.structure.addRule(r)
    }

    /**
     * Adds a definition to the current context.
     */
    fun define(name: QualifiedName, f: Formula, g: Formula, description: String): Rule {
        when (f) {
            is NamedFormula -> {
                require(f.parameters.all { it is VarTerm })
            }
            is PredicateFormula -> {
                require(f.terms.all { it is VarTerm })
            }
            else ->
                throw IllegalArgumentException()

        }

        val rule = MatcherEquivRule.fromFormulas(name, f, g, description)
        addRule(rule)
        return rule
    }

    /**
     * Tries to deduce the required formula [f] using the given rule hints.
     */
    fun deduce(f: Formula, hints: RuleHints): Deduction? {
        val structure = context.structure
        val candidate = structure.ruleMap[hints.ruleName]
        val rules = if (candidate != null) {
            listOf(candidate)
        } else {
            baseStructure.defaultRules.values
        }
        for (rule in rules) {
            val tr = rule.applyToward(context.formulaContext, emptyList(), emptyList(), f)
            if (tr is Reached) {
                addFormula(f)
                return tr.result
            }
        }

        return null
    }


}