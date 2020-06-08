package cn.ancono.mpf.core

import cn.ancono.mpf.builder.RefFormulaContext
import cn.ancono.mpf.matcher.FormulaMatcher
import cn.ancono.mpf.matcher.FormulaMatcherContext
import cn.ancono.mpf.matcher.buildMatcher
import java.util.*
import kotlin.collections.ArrayList


interface LogicRule : Rule {


    fun applyIncremental(
        context: FormulaContext,
        obtained: SortedSet<Formula>,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult

    override fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult {
        return applyIncremental(context, context.regularForms, formulas, terms, desiredResult)
    }

}

open class LogicMatcherRule(
    name: String,
    description: String,
    matcher: FormulaMatcher,
    replacer: RefFormulaContext.() -> Formula
) : MatcherRule(name, description, matcher, replacer), LogicRule {
    override fun applyIncremental(
        context: FormulaContext,
        obtained: SortedSet<Formula>,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult {
        val results = arrayListOf<Formula>()
        for (f in obtained) {
            val r = applyOne(f)
            if (r.any { it.isIdentityTo(desiredResult) }) {
                return Reached(desiredResult)
            }
            results.addAll(r)
        }
        return NotReached(results)
    }

    override fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult {
        return applyIncremental(context, context.regularForms, formulas, terms, desiredResult)
    }
}

open class LogicDefRule(
    name: String,
    description: String,
    m1: FormulaMatcher,
    r1: RefFormulaContext.() -> Formula,
    m2: FormulaMatcher,
    r2: RefFormulaContext.() -> Formula
) : MatcherDefRule(name, description, m1, r1, m2, r2), LogicRule {
    override fun applyIncremental(
        context: FormulaContext,
        obtained: SortedSet<Formula>,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult {
        val results = arrayListOf<Formula>()
        for (f in obtained) {
            val r = applyOne(f)
            if (r.any { it.isIdentityTo(desiredResult) }) {
                return Reached(desiredResult)
            }
            results.addAll(r)
        }
        return NotReached(results)
    }

    override fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): RuleResult {
        return applyIncremental(context, context.regularForms, formulas, terms, desiredResult)
    }
}

/*
 * Created by liyicheng at 2020-05-04 13:40
 */
object LogicRules {
    object RuleFlatten : LogicRule {
        override val name: String
            get() = "Flatten And/Or"

        override val description: String
            get() = "(A∧B)∧C ⇒ A∧B∧C, (A∨B)∨C ⇒ A∨B∨C"

        override fun applyToward(
            context: FormulaContext,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): RuleResult {
            val fs = context.regularForms
            val flattened = desiredResult.flatten()
            val results = ArrayList<Formula>(fs.size)
            for (f in fs) {
                val f1 = f.flatten()
                if (f1.isIdentityTo(flattened)) {
                    return Reached(desiredResult)
                }
                results += f1
            }

            return NotReached(results)
        }

        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Formula> {
            return context.formulas.map { it.flatten() }
        }

        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): RuleResult {
            val results = ArrayList<Formula>(obtained.size)
            val flattened = desiredResult.flatten()
            for (f in obtained) {
                val f1 = f.flatten()
                if (f1.isIdentityTo(flattened)) {
                    return Reached(desiredResult)
                }
                results += f1
            }

            return NotReached(results)
        }
    }

    private fun of(
        matcher: FormulaMatcherContext.() -> FormulaMatcher, replacer: RefFormulaContext.() -> Formula,
        name: String, description: String = "None"
    ): LogicRule {
        return LogicMatcherRule(name, description, buildMatcher(matcher), replacer)
    }

    private fun def(
        m1: FormulaMatcherContext.() -> FormulaMatcher, r1: RefFormulaContext.() -> Formula,
        m2: FormulaMatcherContext.() -> FormulaMatcher, r2: RefFormulaContext.() -> Formula,
        name: String, description: String = "None"
    ): LogicRule = LogicDefRule(name, description, buildMatcher(m1), r1, buildMatcher(m2), r2)


    val RuleDoubleNegative = of({ !!P }, { P }, "Double negative", "!!P => P")

    val RuleIdentityAnd = of({ andF(Q, P, P) }, { Q and P }, "Identity:And", "!!P => P")
    val RuleIdentityOr = of({ orF(Q, P, P) }, { Q or P }, "Identity:Or")

    val RuleAbsorptionAnd = of({ andF(R, P, P or Q) }, { R and P }, "Absorption:And")
    val RuleAbsorptionOr = of({ orF(R, P, P and Q) }, { R or P }, "Absorption:And")

    object RuleAndConstruct : LogicRule {
        override val name: String
            get() = "Construction And"
        override val description: String
            get() = "P,Q => P&Q"

        override fun applyToward(
            context: FormulaContext,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): RuleResult {
            if (desiredResult !is AndFormula) {
                return NotReached(emptyList())
            }
            val children = desiredResult.children
            if (context.regularForms.containsAll(children)) {
                return Reached(desiredResult)
            }
            return NotReached(emptyList())
        }

        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Formula> {
            return emptyList()
        }

        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): RuleResult {
            if (desiredResult !is AndFormula) {
                return NotReached(emptyList())
            }
            val children = desiredResult.children
            if (children.all { context.regularForms.contains(it) || obtained.contains(it) }) {
                return Reached(desiredResult)
            }
            return NotReached(emptyList())
        }
    }

    val RuleAndPart = of({ andF(Q, P) }, { P }, "And part", "P&Q => P")

    //    val Rule
    val RuleImplyCompose = of({ (P implies Q) and (Q implies R) }, { P implies R }, "Imply composition",
        "P->Q and Q->R => P->R"
    )


    val RuleImplyDef = def({ P implies Q }, { !P or Q },
        { !P or Q }, { P implies Q },
        "Def Imply", "P->Q <=> !P & Q"
    )


    object RuleImply : LogicRule {

        val matcher = buildMatcher { P implies Q }

        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): RuleResult {
            val fs = context.regularForms
            val results = obtained.flatMap { f ->
                matcher.replaceOne(f) {
                    if (fs.contains(P.toRegularForm())) {
                        Q
                    } else {
                        null
                    }
                }
            }
            return if (results.any { it.isIdentityTo(desiredResult) }) {
                Reached(desiredResult)
            } else {
                NotReached(results)
            }
        }

        override val name: String
            get() = "Imply"
        override val description: String
            get() = "P,P->Q => Q"


        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Formula> {
            val fs = context.regularForms
            return context.formulas.flatMap { f ->
                matcher.replaceOne(f) {
                    if (fs.contains(P.toRegularForm())) {
                        Q
                    } else {
                        null
                    }
                }
            }
        }
    }


    val RuleEquivToDef = def({ (P implies Q) and (Q implies P) }, { P equivTo Q },
        { P equivTo Q }, { (P implies Q) and (Q implies P) },
        "Def EquivTo", "(P->Q & Q->P) <=> P<->Q"
    )

    val RuleEqualReplace = of({ (x equalTo y) and phi(x) }, { phi(y) }, "Equal replacing",
        "x=y & phi(x) => phi(y)"
    )


    object AllLogicRule : Rule {
        val rules = listOf(
            RuleFlatten,
            RuleDoubleNegative,
            RuleIdentityAnd,
            RuleIdentityOr,
            RuleAbsorptionAnd,
            RuleAbsorptionOr,
            RuleAndConstruct,
            RuleAndPart,
            RuleImplyCompose,
            RuleImplyDef,
            RuleImply,
            RuleEquivToDef,
            RuleEqualReplace
        )
        override val name: String
            get() = "Logic"
        override val description: String
            get() = "Combination of all logic rules."

        var searchDepth = 3

        @Suppress("NAME_SHADOWING")
        override fun applyToward(
            context: FormulaContext,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): RuleResult {
            val dr = desiredResult.toRegularForm()
            var context = context
            val obtained = sortedSetOf(FormulaComparator)
            obtained.addAll(context.formulas)
            val results = arrayListOf<Formula>()
            for (i in 0 until searchDepth) {
                var applied = false
                for (rule in rules) {
                    val re = rule.applyIncremental(context, obtained, formulas, terms, dr)
                    when (re) {
                        is Reached -> return Reached(desiredResult)
                        is NotReached -> {
                            for (f in re.results) {
                                val fr = f.toRegularForm()

                                if (fr !in context.regularForms) {
                                    applied = true
                                    obtained += fr
                                }
                            }
                        }
                    }
                }
                if (!applied) {
                    break
                }
                context = context.addAll(obtained)
                results.addAll(obtained)
                obtained.clear()
            }
            return NotReached(results)
        }

        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Formula> {
            return emptyList()
        }

    }

}

