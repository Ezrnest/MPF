package cn.ancono.mpf.core

import cn.ancono.mpf.builder.RefFormulaContext
import cn.ancono.mpf.builder.SimpleFormulaContext
import cn.ancono.mpf.builder.buildFormula
import cn.ancono.mpf.matcher.FormulaMatcher
import cn.ancono.mpf.matcher.FormulaMatcherContext
import cn.ancono.mpf.matcher.buildMatcher
import java.util.*
import kotlin.collections.ArrayList


interface LogicRule : Rule {

    /**
     * Applies this rule to the given context and the currently obtained formulas.
     *
     * This method is designed for applying logic rules for more than one step.
     *
     * @param obtained obtained formulas in regular form
     */
    fun applyIncremental(
        context: FormulaContext,
        obtained: SortedSet<Formula>,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): TowardResult

    override fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): TowardResult {
        return applyIncremental(context, context.regularForms.navigableKeySet(), formulas, terms, desiredResult)
    }

}

open class LogicMatcherRule(
    name: QualifiedName,
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
    ): TowardResult {
        val results = arrayListOf<Result>()
        for (f in obtained) {
            val replaced = applyOne(f)
            val ctx = listOf(f)
            for (g in replaced) {
                val re = Result(g, ctx)
                if (g.isIdentityTo(desiredResult)) {
                    return Reached(desiredResult, ctx)
                }
                results.add(re)
            }
        }
        return NotReached(results)
    }

    override fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): TowardResult {
        return applyIncremental(context, context.regularForms.navigableKeySet(), formulas, terms, desiredResult)
    }
}

open class LogicDefRule(
    name: QualifiedName,
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
    ): TowardResult {
        val results = arrayListOf<Result>()
        for (f in obtained) {
            val replaced = applyOne(f)
            val ctx = listOf(f)
            for (g in replaced) {
                val re = Result(g, ctx)
                if (g.isIdentityTo(desiredResult)) {
                    return Reached(desiredResult, ctx)
                }
                results.add(re)
            }
        }
        return NotReached(results)
    }

    override fun applyToward(
        context: FormulaContext,
        formulas: List<Formula>,
        terms: List<Term>,
        desiredResult: Formula
    ): TowardResult {
        return applyIncremental(context, context.regularForms.navigableKeySet(), formulas, terms, desiredResult)
    }
}

/**
 * Contains all the rules of first order logic.
 * Created by liyicheng at 2020-05-04 13:40
 */
object LogicRules {
    val LogicNamespace = "logic"

    private fun nameOf(n: String): QualifiedName {
        return QualifiedName.of(n, LogicNamespace)
    }

    object RuleFlatten : LogicRule {
        override val name: QualifiedName = nameOf("Flatten")

        override val description: String
            get() = "(A∧B)∧C ⇒ A∧B∧C, (A∨B)∨C ⇒ A∨B∨C"

        override fun applyToward(
            context: FormulaContext,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
            val fs = context.regularForms.keys
            val flattened = desiredResult.flatten().regularForm
            val results = ArrayList<Result>(fs.size)
            for ((fr, f) in context.regularForms) {
                val f1 = fr.flatten()
                if (f1.isIdentityTo(flattened)) {
                    return Reached(desiredResult, listOf(f))
                }
                results += Result(desiredResult, listOf(f))
            }

            return NotReached(results)
        }

        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Result> {
            return context.formulas.map { Result(it.flatten(), listOf(it)) }
        }

        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
            val results = ArrayList<Result>(obtained.size)
            val flattened = desiredResult.flatten().regularForm
            for (f in obtained) {
                val f1 = f.flatten().regularForm
                if (f1.isIdentityTo(flattened)) {
                    return Reached(desiredResult, listOf(f))
                }
                results += Result(desiredResult, listOf(f))
            }

            return NotReached(results)
        }
    }

    private fun of(
        matcher: FormulaMatcherContext.() -> FormulaMatcher, replacer: RefFormulaContext.() -> Formula,
        name: String, description: String = "None"
    ): LogicRule {
        return LogicMatcherRule(nameOf(name), description, buildMatcher(matcher), replacer)
    }

    private fun def(
        p: SimpleFormulaContext.() -> Formula,
        q: SimpleFormulaContext.() -> Formula,
        name: String, description: String = "None"
    ): LogicRule {
        val f1 = buildFormula(p)
        val f2 = buildFormula(q)
        val m1 = FormulaMatcher.fromFormula(f1)
        val m2 = FormulaMatcher.fromFormula(f2)
        val r1: (RefFormulaContext.() -> Formula) = {
            f2.replaceVar { v ->
                termContext.context[v.name]?.term!!
            }.replaceNamed { nf ->
                formulas[nf.name.fullName]?.formula!!
            }
        }
        val r2: (RefFormulaContext.() -> Formula) = {
            f1.replaceVar { v ->
                termContext.context[v.name]?.term!!
            }.replaceNamed { nf ->
                formulas[nf.name.fullName]?.formula!!
            }
        }
        return LogicDefRule(nameOf(name), description, m1, r1, m2, r2)
    }


    val RuleDoubleNegate = of({ !!P }, { P }, "DoubleNegate", "!!P => P")

    val RuleIdentityAnd = of({ andF(Q, P, P) }, { Q and P }, "IdentityAnd", "P&P => P")
    val RuleIdentityOr = of({ orF(Q, P, P) }, { Q or P }, "IdentityOr", "P|P => P")

    val RuleAbsorptionAnd = of({ andF(R, P, P or Q) }, { R and P }, "AbsorptionAnd", "P&(P|Q) => P")
    val RuleAbsorptionOr = of({ orF(R, P, P and Q) }, { R or P }, "AbsorptionOr", "P|(P&Q) => P")

    object RuleAndConstruct : LogicRule {
        override val name: QualifiedName = nameOf("ConstructAnd")
        override val description: String
            get() = "P,Q => P&Q"

        override fun applyToward(
            context: FormulaContext,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
            if (desiredResult !is AndFormula) {
                return NotReached(emptyList())
            }
            val children = desiredResult.children

            val rf = context.regularForms
            if (children.all { c -> c.regularForm in rf }) {
                val ctx = children.map { c ->
                    context.regularForms[c.regularForm]!!
                }
                return Reached(desiredResult, ctx)
            }
            return NotReached(emptyList())
        }

        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Result> {
            return emptyList()
        }

        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
            if (desiredResult !is AndFormula) {
                return NotReached(emptyList())
            }
            val children = desiredResult.children
            if (children.all { context.regularForms.contains(it) || obtained.contains(it) }) {
                val allContext = context.formulas.asSequence() + obtained.asSequence()
                val ctx = children.map { c ->
                    allContext.first { f -> c.isIdentityTo(f) }
                }
                return Reached(desiredResult, ctx)
            }
            return NotReached(emptyList())
        }
    }

    val RuleAndProject = of({ andF(Q, P) }, { P }, "AndProject", "P&Q => P")

    //    val Rule
    val RuleImplyCompose = of({ (P implies Q) and (Q implies R) }, { P implies R }, "ImplyCompose",
        "P->Q and Q->R => P->R"
    )


    val RuleDefImply = def({ P implies Q }, { !P or Q },
//        { !P or Q }, { P implies Q },
        "DefImply", "P->Q <=> !P | Q"
    )


    object RuleImply : LogicRule {

        val matcher = buildMatcher { P implies Q }

        /**
         * @param rf regular form map
         */
        private fun buildResults(
            context: Collection<Formula>,
            rf: NavigableMap<Formula, Formula>
        ): List<Pair<Formula, Formula>> {
            return context.flatMap { f ->
                matcher.replaceOneWith(f) {
                    val pr = P.regularForm
                    if (rf.contains(pr)) {
                        Q to rf[pr]!!
                    } else {
                        null
                    }
                }
            }
        }

        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
            val dr = desiredResult.regularForm
            val rf = context.regularForms
            val results = buildResults(obtained, rf)
            for (re in results) {
                if (re.first.regularForm.isIdentityTo(dr)) {
                    return Reached(re.first, listOf(re.second))
                }
            }
            return NotReached(results.map {
                Result(it.first, listOf(it.second))
            })
        }

        override val name: QualifiedName = nameOf("Imply")
        override val description: String
            get() = "P,P->Q => Q"


        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Result> {
            val rf = context.regularForms
            return buildResults(context.formulas, rf).map {
                Result(it.first, listOf(it.second))
            }
        }
    }


    val RuleEquivToDef = def({ (P implies Q) and (Q implies P) }, { P equivTo Q },
//        { P equivTo Q }, { (P implies Q) and (Q implies P) },
        "DefEquivTo", "(P->Q & Q->P) <=> P<->Q"
    )

    val RuleEqualReplace = of({ (x equalTo y) and phi(x) }, { phi(y) }, "EqualReplace",
        "x=y & phi(x) => phi(y)"
    )

    object RuleExcludeMiddle : LogicRule {

        override val name: QualifiedName = nameOf("ExcludeMiddle")
        override val description: String
            get() = "=> P or !P"

        val matcher = buildMatcher { P or !P }

        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
            val matchResults = matcher.match(desiredResult)
            if (matchResults.isNotEmpty()) {
                return Reached(desiredResult, emptyList())
            }
            return NotReached(emptyList())
        }


        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Result> {
            if (formulas.isEmpty()) {
                return emptyList()
            }
            return formulas.map { f ->
                val nf = buildFormula { f or !f }
                Result(nf, emptyList())
            }
        }
    }

    object RuleExistConstant : LogicRule {

        override val name: QualifiedName = nameOf("ExistConstant")
        override val description: String
            get() = "phi(c) => exist[x] phi(x)"

        private fun apply(f: Formula): List<Pair<Formula, Constant>> {
            val constants = f.allConstants()
            val newVariable = Formula.nextVar(f)
            val varTerm = VarTerm(newVariable)
            return constants.map { c ->
                buildFromConstant(f, c, newVariable, varTerm)
            }
        }

        private fun buildFromConstant(f: Formula, c: Constant, nv: Variable, vt: VarTerm): Pair<Formula, Constant> {
            val sub = f.recurMapTerm { t ->
                if (t is ConstTerm && t.c == c) {
                    vt
                } else {
                    t
                }
            }
            val nf = ExistFormula(sub, nv)
            return nf to c
        }


        override fun applyIncremental(
            context: FormulaContext,
            obtained: SortedSet<Formula>,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
            // apply to the given constant term if exists
            val givenConstants = terms.filterIsInstance<ConstTerm>().map { it.c }

            val allResults = arrayListOf<Result>()
            for (f in context.formulas + obtained.toList()) {
                val newVariable = Formula.nextVar(f)
                val varTerm = VarTerm(newVariable)
                val constants = if (givenConstants.isEmpty()) {
                    f.allConstants()
                } else {
                    givenConstants
                }
                for (c in constants) {
                    val (rf, constant) = buildFromConstant(f, c, newVariable, varTerm)
                    val regular = rf.regularForm
                    if (regular.isIdentityTo(desiredResult.regularForm)) {
                        return Reached(desiredResult, listOf(f), mapOf("constant" to constant))
                    }
                    if (regular !in obtained) {
                        val result = Result(rf, listOf(f), mapOf("constant" to constant))
                        allResults.add(result)
                    }
                }

            }
            return NotReached(allResults)
        }


        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Result> {
            val givenConstants = terms.filterIsInstance<ConstTerm>().map { it.c }

            val allResults = arrayListOf<Result>()
            for (f in context.formulas) {
                val newVariable = Formula.nextVar(f)
                val varTerm = VarTerm(newVariable)
                val constants = if (givenConstants.isEmpty()) {
                    f.allConstants()
                } else {
                    givenConstants
                }
                for (c in constants) {
                    val (rf, constant) = buildFromConstant(f, c, newVariable, varTerm)
                    val result = Result(rf, listOf(f), mapOf("constant" to constant))
                    allResults.add(result)
                }

            }
            return allResults
        }
    }

    /**
     * The list of all the logic rules.
     */
    val Rules = listOf(
        RuleFlatten,
        RuleDoubleNegate,
        RuleIdentityAnd,
        RuleIdentityOr,
        RuleAbsorptionAnd,
        RuleAbsorptionOr,
        RuleAndConstruct,
        RuleAndProject,
        RuleImplyCompose,
        RuleDefImply,
        RuleImply,
        RuleEquivToDef,
        RuleEqualReplace,
        RuleExcludeMiddle
    )

    /**
     * A rule that tries to apply all the viable logic rules for multiple steps.
     */
    object AllLogicRule : Rule {

        override val name: QualifiedName
            get() = nameOf("Logic")
        override val description: String
            get() = "Combination of all logic rules."

        var searchDepth = 3


        @Suppress("NAME_SHADOWING")
        override fun applyToward(
            context: FormulaContext,
            formulas: List<Formula>,
            terms: List<Term>,
            desiredResult: Formula
        ): TowardResult {
//            val dr = desiredResult.regularForm
            val context = context.copy()

            val reached = TreeMap<Formula, List<Formula>>(FormulaComparator)
            for (en in context.regularForms) {
                reached[en.key] = listOf(en.value)
            }
            var obtained: SortedSet<Formula> = TreeSet(reached.navigableKeySet()) // formulas obtained in each loop
            for (i in 0 until searchDepth) {
                var applied = false
                val newObtained = sortedSetOf(FormulaComparator)
                for (rule in Rules) {
                    val towardResult =
                        rule.applyIncremental(context, obtained, formulas, terms, desiredResult)
                    when (towardResult) {
                        is Reached -> {
                            val re = towardResult.result
                            val ctx = re.dependencies.flatMap {
                                reached[it.regularForm]!!
                            } // recursive dependencies
                            return Reached(desiredResult, ctx)
                        }
                        is NotReached -> {
                            for (re in towardResult.results) {
                                val f = re.f
                                val fr = f.regularForm
                                if (fr !in reached) {
                                    applied = true
                                    reached[fr] = re.dependencies.flatMap {
                                        reached[it.regularForm]!!
                                    }
                                    newObtained += fr
                                }
                            }
                        }
                    }
                }
                if (!applied) {
                    break
                }
                context.addAll(obtained)
                obtained.clear()
                obtained = newObtained
            }
            return NotReached(emptyList())
        }

        override fun apply(context: FormulaContext, formulas: List<Formula>, terms: List<Term>): List<Result> {
            return emptyList()
        }

    }


}

