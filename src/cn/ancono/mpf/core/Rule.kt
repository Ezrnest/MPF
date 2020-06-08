package cn.ancono.mpf.core

import cn.ancono.mpf.builder.RefFormulaContext
import cn.ancono.mpf.matcher.FormulaMatcher

/*
 * Created by liyicheng at 2020-04-05 19:10
 */




/**
 * Describes the transformation rule in a structure.
 * @author liyicheng
 */
interface Rule {
    //    val arguments : List<ArgumentType>
    val name: String

    val description: String


    /**
     * Applies this rule to the given context, formulas and terms, and return the result of applying
     * this rule. If this rule is not applicable or the desired result can not be reached, `null` may be returned.
     * @param context formulas that
     */
    fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult

    /**
     * Applies this rule to the given context, formulas and terms, and return a list of possible results of applying
     * this rule. An empty list can be returned if this rule is not suitable.
     * @param context formulas that
     */
    fun apply(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>
    ): List<Formula>


//    /**
//     * A matcher to determine whether the input
//     */
//    val inputMatcher: FormulaMatcher


}

sealed class RuleResult

data class Reached(val result: Formula) : RuleResult()

data class NotReached(val results: List<Formula>) : RuleResult()


open class MatcherRule(
    override val name: String,
    override val description: String,
    val matcher: FormulaMatcher, protected val replacer: RefFormulaContext.() -> Formula
) : Rule {
    override fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult {
        val allResults = arrayListOf<Formula>()
        val fs = context.formulas
        for (i in fs.lastIndex downTo 0) {
            val f = fs[i]
            val results = applyOne(f)
            for (r in results) {
                if (r.isIdentityTo(desiredResult)) {
                    return Reached(desiredResult)
                }
            }
            allResults.addAll(results)
        }
        return NotReached(allResults)
    }

    protected open fun applyOne(f: Formula): List<Formula> {
        return matcher.replaceOne(f, replacer) + matcher.replaceAll(f, replacer)
    }

    override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Formula> {
        return context.formulas.flatMap { applyOne(it) }
    }
}



open class MatcherDefRule(
    name: String,
    description: String,
    m1: FormulaMatcher, r1: RefFormulaContext.() -> Formula,
    val m2: FormulaMatcher, private val r2: RefFormulaContext.() -> Formula
) : MatcherRule(name,description,m1,r1) {


    override fun applyOne(f: Formula): List<Formula> {
        val re = arrayListOf<Formula>()
        re.addAll(matcher.replaceOne(f,replacer))
        re.add(matcher.replaceAll(f,replacer))
        re.addAll(m2.replaceOne(f,r2))
        re.add(m2.replaceAll(f,r2))
        return re
    }

}