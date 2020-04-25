package cn.ancono.mpf.core

import cn.ancono.mpf.matcher.AtomicMatcher
import cn.ancono.mpf.matcher.MatchResult
import cn.ancono.mpf.matcher.Matcher
import cn.ancono.mpf.matcher.MatcherUtil


typealias FMap = Map<String, Formula>
typealias TMap = Map<String, Term>

/*
 * Created by liyicheng at 2020-04-05 15:05
 */
interface FormulaMatcher : Matcher<Formula, FormulaMatchResult> {
//    fun fullMatches(f: Formula, formulaMap: FMap = emptyMap(), varMap: TMap = emptyMap()): FullFormulaMatchResult?

//    fun partMatches(f: Formula): FormulaMatchResult?


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
}

class FormulaMatchResult(
    val formulaMap: FMap,
    val varMap: TMap
) : MatchResult {

    fun replace(builderAction: FormulaBuilderContext.() -> Formula): Formula {
        val context = toBuilderContext()
        return builderAction(context)
    }

    fun toBuilderContext(): FormulaBuilderContext = FormulaBuilderContext(formulaMap, TermBuilderContext(varMap))
}

val FormulaMatchResult?.destructed: Pair<FMap, TMap>
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

class VariableMatcher(val name: String) : FormulaMatcher, AtomicMatcher<Formula, FormulaMatchResult> {

    override fun match(x: Formula, previousResult: FormulaMatchResult?): FormulaMatchResult? {
        val (formulaMap, varMap) = previousResult.destructed
        val required = formulaMap[name]
        return if (required == null) {
            FormulaMatchResult(formulaMap + (name to x), varMap)
        } else {
            if (required.isIdentityTo(x)) {
                FormulaMatchResult(formulaMap, varMap)
            } else {
                null
            }
        }
    }
}

class NamedFormulaMatcher(val name: QualifiedName) : FormulaMatcher {

    override fun match(x: Formula, previousResult: FormulaMatchResult?): FormulaMatchResult? {
        val (formulaMap, varMap) = previousResult.destructed
        return if (x is NamedFormula && x.name == name) {
            FormulaMatchResult(formulaMap, varMap)
        } else {
            null
        }
    }
}

class PredicateFormulaMatcher(val predicate: Predicate, val termMatchers: List<TermMatcher>, val ordered: Boolean) :
    FormulaMatcher {

    override fun match(x: Formula, previousResult: FormulaMatchResult?): FormulaMatchResult? {
        val (formulaMap, varMap) = previousResult.destructed
        if (x is PredicateFormula && x.p == predicate) {
            val result = if (ordered) {
                MatcherUtil.orderedMatch(x.terms, termMatchers, TermMatchResult(varMap))
            } else {
                MatcherUtil.unorderedMatch(x.terms, termMatchers, TermMatchResult(varMap))
            } ?: return null
            return FormulaMatchResult(formulaMap, varMap + result.varMap)
        }else{
            return null
        }
    }
}

//class UnaryFormulaMatcher(val type : Class<*>,)