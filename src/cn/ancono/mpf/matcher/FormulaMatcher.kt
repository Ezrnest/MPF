package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.*


typealias FMap = Map<String, Formula>
typealias TMap = Map<String, Term>

/*
 * Created by liyicheng at 2020-04-05 15:05
 */
interface FormulaMatcher : Matcher<Formula, FormulaResult> {
//    fun fullMatches(f: Formula, formulaMap: FMap = emptyMap(), varMap: TMap = emptyMap()): FullFormulaMatchResult?

    //    fun partMatches(f: Formula): FormulaMatchResult?
    override fun match(x: Formula, previousResult: FormulaResult?): FormulaResult?

    fun replaceOne(f: Formula, builderAction: FormulaBuilderContext.() -> Formula): List<Formula> {
        return f.recurMapMulti {
            val re = match(it)
            if (re != null) {
                listOf(re.replace(builderAction))
            } else {
                emptyList()
            }
        }
    }

    fun replaceAll(f : Formula, builderAction: FormulaBuilderContext.() -> Formula): Formula {
        return f.recurMap {
            val re = match(it)
            re?.replace(builderAction) ?: it
        }
    }

}

class FormulaResult(
    val formulaMap: FMap,
    val varMap: TMap
) : MatchResult {

    fun replace(builderAction: FormulaBuilderContext.() -> Formula): Formula {
        val context = toBuilderContext()
        return builderAction(context).flatten()
    }

    fun toBuilderContext(): FormulaBuilderContext =
        FormulaBuilderContext(
            formulaMap,
            TermBuilderContext(varMap)
        )

    override fun toString(): String {
        return "Formulas: $formulaMap, Variables: $varMap"
    }
}

val FormulaResult?.destructed: Pair<FMap, TMap>
    get() = Pair(this?.formulaMap ?: emptyMap(), this?.varMap ?: emptyMap())

//
//class FullFormulaMatchResult(val target: Formula, formulaMap: FMap, varMap: TMap) : FormulaMatchResult(formulaMap, varMap) {
//    override fun replace(builderAction: FormulaBuilderContext.() -> Formula): Formula {
//        val context = toBuilderContext()
//        return builderAction(context)
//    }
//}
//
//class PartFormulaMatchResult(val root: CombinedFormula, val childIndex: Set<Int>, formulaMap: FMap, varMap: TMap) :
//    FormulaMatchResult(formulaMap, varMap) {
//
//    override fun replace(builderAction: FormulaBuilderContext.() -> Formula): Formula {
//        val result = ArrayList<Formula>(root.childCount - childIndex.size + 1)
//        for ((i, c) in root.children.withIndex()) {
//            if (i !in childIndex) {
//                result += c
//            }
//        }
//        val build = builderAction(toBuilderContext())
//        result.add(build)
//        return root.copyOf(result)
//    }
//}

object EmptyMatcher : FormulaMatcher {
    override fun match(x: Formula, previousResult: FormulaResult?): FormulaResult? {
        return null
    }
}

class RefFormulaMatcher(val name: String) : FormulaMatcher, AtomicMatcher<Formula, FormulaResult> {

    override fun match(x: Formula, previousResult: FormulaResult?): FormulaResult? {
        val (formulaMap, varMap) = previousResult.destructed
        val required = formulaMap[name]
        return if (required == null) {
            FormulaResult(formulaMap + (name to x), varMap)
        } else {
            if (required.isIdentityTo(x)) {
                FormulaResult(formulaMap, varMap)
            } else {
                null
            }
        }
    }
}

class NamedFormulaMatcher(val name: QualifiedName) :
    FormulaMatcher {

    override fun match(x: Formula, previousResult: FormulaResult?): FormulaResult? {
        val (formulaMap, varMap) = previousResult.destructed
        return if (x is NamedFormula && x.name == name) {
            FormulaResult(formulaMap, varMap)
        } else {
            null
        }
    }
}

class PredicateFormulaMatcher(val predicate: Predicate, val termMatchers: List<TermMatcher>, val ordered: Boolean = true) :
    FormulaMatcher {

    override fun match(x: Formula, previousResult: FormulaResult?): FormulaResult? {
        val (formulaMap, varMap) = previousResult.destructed
        if (x is PredicateFormula && x.p == predicate) {
            val result = if (ordered) {
                MatcherUtil.orderedMatch(
                    x.terms, termMatchers,
                    TermMatchResult(varMap)
                )
            } else {
                MatcherUtil.unorderedMatch(
                    x.terms, termMatchers,
                    TermMatchResult(varMap)
                )
            } ?: return null
            return FormulaResult(formulaMap, varMap + result.varMap)
        } else {
            return null
        }
    }
}

class NotFormulaMatcher(subMatcher: FormulaMatcher) :
    UnaryMatcher<Formula, FormulaResult>(
        NotFormula::class.java, subMatcher
    ),
    FormulaMatcher

class ImplyFormulaMatcher(sub1: FormulaMatcher, sub2: FormulaMatcher) :
    OrderedMatcher<Formula, FormulaResult>(
        ImplyFormula::class.java, listOf(sub1, sub2)
    ), FormulaMatcher

class EquivalentFormulaMatcher(sub1: FormulaMatcher, sub2: FormulaMatcher) :
    UnorderedMatcher<Formula, FormulaResult>(
        EquivalentFormula::class.java, listOf(sub1, sub2),
        EmptyMatcher
    ), FormulaMatcher

open class QualifiedFormulaMatcher(
    val type: Class<out QualifiedFormula>,
    val varName: String,
    val sub: FormulaMatcher
) :
    FormulaMatcher {
    override fun match(x: Formula, previousResult: FormulaResult?): FormulaResult? {
        if (x !is QualifiedFormula || !type.isInstance(x)) {
            return null
        }
        val variable = x.v
        val (formulaMap, varMap) = previousResult.destructed
        val nVarMap = varMap + (varName to VarTerm(variable))
        return sub.match(x.child, FormulaResult(formulaMap, nVarMap))
    }
}

class ExistFormulaMatcher(varName: String, sub: FormulaMatcher) :
    QualifiedFormulaMatcher(ExistFormula::class.java, varName, sub), FormulaMatcher

class ForAnyFormulaMatcher(varName: String, sub: FormulaMatcher) :
    QualifiedFormulaMatcher(ForAnyFormula::class.java, varName, sub), FormulaMatcher

class AndFormulaMatcher(subMatchers: List<FormulaMatcher>, fallback: FormulaMatcher) :
    UnorderedMatcher<Formula, FormulaResult>(
        AndFormula::class.java, subMatchers, fallback
    ), FormulaMatcher

class OrFormulaMatcher(subMatchers: List<FormulaMatcher>, fallback: FormulaMatcher) :
    UnorderedMatcher<Formula, FormulaResult>(
        OrFormula::class.java, subMatchers, fallback
    ), FormulaMatcher