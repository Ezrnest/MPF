package cn.ancono.mpf.matcher

import cn.ancono.mpf.builder.RefFormulaContext
import cn.ancono.mpf.builder.RefTermContext
import cn.ancono.mpf.core.*


typealias FMap = Map<String, Formula>
typealias TMap = Map<String, Term>


/*
 * Created by liyicheng at 2020-04-05 15:05
 */
interface FormulaMatcher : Matcher<Formula, FormulaResult> {
//    fun fullMatches(f: Formula, formulaMap: FMap = emptyMap(), varMap: TMap = emptyMap()): FullFormulaMatchResult?

    //    fun partMatches(f: Formula): FormulaMatchResult?
    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult>

//    {
//        val results = arrayListOf<FormulaResult>()
//        if (previousResult == null || previousResult.isEmpty()) {
//            return matchSingle(x)
//        }
//        for (pr in previousResult) {
//            val result = matchSingle(x, pr)
//            if (result != null) {
//                results.addAll(result)
//            }
//        }
//        return results
//    }


    open fun replaceOne(f: Formula, builderAction: RefFormulaContext.() -> Formula): List<Formula> {
        return f.recurMapMulti {
            val res = match(it, null)
            res.map { re -> re.replace(builderAction) }
        }
    }

    open fun replaceAll(f: Formula, builderAction: RefFormulaContext.() -> Formula): Formula {
        return f.recurMap {
            val re = match(it, null)
            if (re.isEmpty()) {
                it
            } else {
                re.first().replace(builderAction)
            }
        }
    }
}


class FormulaResult(
    val formulaMap: FMap,
    val varMap: TMap
) : MatchResult {

    fun replace(builderAction: RefFormulaContext.() -> Formula): Formula {
        val context = toBuilderContext()
        return builderAction(context).flatten()
    }

    fun toBuilderContext(): RefFormulaContext =
        RefFormulaContext(
            formulaMap,
            RefTermContext(varMap)
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
    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        return emptyList()
    }
    //    override fun matchSingle(x: Formula, previousResult: FormulaResult?): FormulaResults? {
//        return null
//    }
}

class RefFormulaMatcher(val name: String) : FormulaMatcher, AtomicMatcher<Formula, FormulaResult> {

    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        val (formulaMap, varMap) = previousResult.destructed
        val required = formulaMap[name]
        return if (required == null) {
            listOf(FormulaResult(formulaMap + (name to x), varMap))
        } else {
            if (required.isIdentityTo(x)) {
                listOf(FormulaResult(formulaMap, varMap))
            } else {
                emptyList()
            }
        }
    }
}

class NamedFormulaMatcher(val name: QualifiedName) :
    FormulaMatcher {
    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        val (formulaMap, varMap) = previousResult.destructed
        return if (x is NamedFormula && x.name == name) {
            listOf(FormulaResult(formulaMap, varMap))
        }else{
            emptyList()
        }
    }

}

class PredicateFormulaMatcher(
    val predicate: Predicate,
    val termMatchers: List<TermMatcher>,
    val ordered: Boolean = true
) :
    FormulaMatcher {
    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        val (formulaMap, varMap) = previousResult.destructed
        if (x !is PredicateFormula || x.p != predicate) {
            return emptyList()
        }
        val results = if (ordered) {
            MatcherUtil.orderedMatch(
                x.terms, termMatchers,
                TermMatchResult(varMap)
            )
        } else {
            MatcherUtil.unorderedMatch(
                x.terms, termMatchers,
                TermMatchResult(varMap)
            )
        }
        return results.map { r -> FormulaResult(formulaMap, varMap + r.varMap) }
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

/**
 * Creates a qualified formula matcher. The matcher will not store the qualified variable (named as
 * [varName]) in the matching result.
 */
open class QualifiedFormulaMatcher(
    val type: Class<out QualifiedFormula>,
    val varName: String,
    val sub: FormulaMatcher
) :
    FormulaMatcher {

    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        if (x !is QualifiedFormula || !type.isInstance(x)) {
            return emptyList()
        }
        val variable = x.v
        val (formulaMap, varMap) = previousResult.destructed
        val nVarMap = varMap + (varName to VarTerm(variable))
        val res = sub.match(x.child, FormulaResult(formulaMap, nVarMap))
        return res.map {re ->
            val (m1, m2) = re.destructed
            val original = varMap[varName]
            val m3 = m2.toMutableMap()
            if (original != null) {
                m3[varName] = original
            } else {
                m3.remove(varName)
            }
            FormulaResult(m1, m3)
        }
    }

}

class ExistFormulaMatcher(varName: String, sub: FormulaMatcher) :
    QualifiedFormulaMatcher(ExistFormula::class.java, varName, sub), FormulaMatcher

class ForAnyFormulaMatcher(varName: String, sub: FormulaMatcher) :
    QualifiedFormulaMatcher(ForAnyFormula::class.java, varName, sub), FormulaMatcher

class AndFormulaMatcher(override val children: List<FormulaMatcher>, override val fallback: FormulaMatcher) :
    UnorderedMatcher<Formula, FormulaResult>(
        AndFormula::class.java, children, fallback
    ), FormulaMatcher

class OrFormulaMatcher(override val children: List<FormulaMatcher>, override val fallback: FormulaMatcher) :
    UnorderedMatcher<Formula, FormulaResult>(
        OrFormula::class.java, children, fallback
    ), FormulaMatcher