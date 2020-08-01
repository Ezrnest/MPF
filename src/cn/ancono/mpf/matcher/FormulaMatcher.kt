package cn.ancono.mpf.matcher

import cn.ancono.mpf.builder.RefFormulaScope
import cn.ancono.mpf.builder.RefTermScope
import cn.ancono.mpf.core.*
import java.lang.UnsupportedOperationException


typealias FMap = Map<String, RefFormula>
typealias TMap = Map<String, RefTerm>


/*
 * Created by liyicheng at 2020-04-05 15:05
 */
interface FormulaMatcher : Matcher<Formula, FormulaResult> {
//    fun fullMatches(f: Formula, formulaMap: FMap = emptyMap(), varMap: TMap = emptyMap()): FullFormulaMatchResult?

    //    fun partMatches(f: Formula): FormulaMatchResult?

    /**
     * Determines whether the formula matcher matches exactly the given formula.
     */
    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult>


    /**
     * Determines whether this matcher matches the formula or any of its sub-formulas.
     */
    fun find(x: Formula): Boolean {
        return x.recurApply {
            match(x, null).isNotEmpty()
        }
    }

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


    open fun replaceOne(f: Formula, builderAction: RefFormulaScope.() -> Formula?): List<Formula> {
        return f.recurMapMulti {
            val res = match(it, null)
            val result = ArrayList<Formula>(res.size)
            for (re in res) {
                val t = re.replace(builderAction)
                if (t != null) {
                    result += t
                }
            }
            result
        }
    }

    fun <T> replaceOneWith(f: Formula, builderAction: RefFormulaScope.() -> Pair<Formula, T>?): List<Pair<Formula, T>> {
        return f.recurMapMultiWith {
            val res = match(it, null)
            val result = ArrayList<Pair<Formula, T>>(res.size)
            for (re in res) {
                val t = re.replaceWith(builderAction)
                if (t != null) {
                    result += t
                }
            }
            result
        }
    }

    open fun replaceAll(f: Formula, builderAction: RefFormulaScope.() -> Formula): Formula {
        return f.recurMap {
            val re = match(it, null)
            if (re.isEmpty()) {
                it
            } else {
                re.first().replaceNonNull(builderAction)
            }
        }
    }


    companion object {
        /**
         * Construct a formula matcher from a formula. This method will
         * convert named formula to formula reference, and convert all the
         * other types of formulas to corresponding types of formula matchers.
         *
         * @param keepNamed whether named formula should be interpreted simply as named
         * formula or as referred formulas in the matcher. If it is `true`, then only
         * formula with exactly the same name will be matched.
         */
        fun fromFormula(f: Formula, keepNamed: Boolean = true): FormulaMatcher {
            when (f) {
                is NamedFormula -> {
                    if (keepNamed) {
                        return NamedFormulaMatcher(f.name, f.parameters.map { TermMatcher.fromTerm(it) })
                    }
                    return VarRefFormulaMatcher(f.name.fullName, f.parameters.map { t ->
                        if (t !is VarTerm) {
                            throw UnsupportedOperationException("Named formula with non-variable term is not supported.")
                        }
                        t.v.name
                    })
                }
                is PredicateFormula ->
                    return PredicateFormulaMatcher(f.p, f.terms.map { TermMatcher.fromTerm(it) }, f.p.ordered)
                is ForAnyFormula ->
                    return ForAnyFormulaMatcher(f.v.name, fromFormula(f.child, keepNamed))
                is ExistFormula ->
                    return ExistFormulaMatcher(f.v.name, fromFormula(f.child, keepNamed))
                is NotFormula ->
                    return NotFormulaMatcher(fromFormula(f.child, keepNamed))
                is ImplyFormula ->
                    return ImplyFormulaMatcher(fromFormula(f.child1, keepNamed), fromFormula(f.child2, keepNamed))
                is EquivalentFormula ->
                    return EquivalentFormulaMatcher(fromFormula(f.child1, keepNamed), fromFormula(f.child2, keepNamed))
                is AndFormula ->
                    return AndFormulaMatcher(f.children.map { fromFormula(it, keepNamed) }, EmptyMatcher)
                is OrFormula ->
                    return OrFormulaMatcher(f.children.map { fromFormula(it, keepNamed) }, EmptyMatcher)
//                else->
//                    throw UnsupportedOperationException()
            }
        }
    }
}


class FormulaResult(
    val formulaMap: FMap,
    val varMap: TMap
) : MatchResult {

    fun get(name: String): Formula {
        return formulaMap[name]?.formula ?: throw NoSuchElementException()
    }

    fun replace(builderAction: RefFormulaScope.() -> Formula?): Formula? {
        val context = toBuilderContext()
        return builderAction(context)?.flatten()
    }

    fun <T> replaceWith(builderAction: RefFormulaScope.() -> Pair<Formula, T>?): Pair<Formula, T>? {
        val context = toBuilderContext()
        val (f, t) = builderAction(context) ?: return null
        return f.flatten() to t
    }


    fun replaceNonNull(builderAction: RefFormulaScope.() -> Formula): Formula {
        val context = toBuilderContext()
        return builderAction(context).flatten()
    }


    fun toBuilderContext(): RefFormulaScope =
        RefFormulaScope(
            formulaMap,
            RefTermScope(varMap)
        )

    override fun toString(): String = buildString {
        append("Formulas: ")
        formulaMap.entries.joinTo(this, ",", "{", "}") { (k, v) ->
            if (v.isClosed) {
                "$k=$v"
            } else {
                k + v.parameters.joinToString(",", prefix = "(", postfix = ")") { it.name } + "=${v.formula}"
            }
        }
        append(", Variables: ")
        varMap.entries.joinTo(this, ",", "{", "}") { (k, v) ->
            if (v.isClosed) {
                "$k=$v"
            } else {
                k + v.parameters.joinToString(",", prefix = "(", postfix = ")") { it.name } + "=${v.term}"
            }
        }
//        return "Formulas: $formulaMap, Variables: $varMap"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FormulaResult

        if (formulaMap != other.formulaMap) return false
        if (varMap != other.varMap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = formulaMap.hashCode()
        result = 31 * result + varMap.hashCode()
        return result
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
}

object WildcardMatcher : FormulaMatcher {
    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        return if (previousResult == null) {
            listOf(FormulaResult(emptyMap(), emptyMap()))
        } else {
            listOf(previousResult)
        }
    }
}

class SimpleRefFormulaMatcher(val name: String) : FormulaMatcher, AtomicMatcher<Formula, FormulaResult> {
    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        val formulaMap = previousResult?.formulaMap ?: emptyMap()
        val varMap = previousResult?.varMap ?: emptyMap()
        val required = formulaMap[name]
        return if (required == null) {
            listOf(FormulaResult(formulaMap + (name to RefFormula(x)), varMap))
        } else {
            if (x.isIdentityTo(required.formula)) {
                listOf(FormulaResult(formulaMap, varMap))
            } else {
                emptyList()
            }
        }
    }
}

/**
 * A reference formula with variable.
 */
class VarRefFormulaMatcher(
    val name: String,
    val varNames: List<String> = emptyList(),
    val sub: FormulaMatcher = WildcardMatcher
) : FormulaMatcher, AtomicMatcher<Formula, FormulaResult> {

    init {
        require(MatcherUtil.isDistinct(varNames)) {
            "Variable names must be distinct!"
        }
    }

    private fun buildReferredAndRemains(varMap: TMap): Pair<MutableMap<String, Term>, MutableList<String>> {
        val referred = hashMapOf<String, Term>()
        val remains = ArrayList<String>(varNames.size)
        for (v in varNames) {
            val ref = varMap[v]
            if (ref != null) {
                referred[v] = ref.build(varMap)
//                require(t is VarTerm){
//                    "Only variable reference is supported now!"
//                }
            } else {
                remains.add(v)
            }
        }
        return referred to remains
    }

    private fun matchRequired(
        x: Formula, required: RefFormula,
        formulaMap: FMap, varMap: TMap
    ): List<FormulaResult> {
        /*
        Required: P(x1,x2,...,xn)
        x:        Q(y1,y2,...,yn)

         */
        val (reference, remains) = buildReferredAndRemains(varMap)
        val referred = reference.values.mapTo(hashSetOf()) {
            require(it is VarTerm) {
                "Only variable reference is supported now."
            }
            it.v
        }
//        val referred = reference.forEach { (key, value) ->
//            require(value is VarTerm) {
//                "Only variable reference is supported now."
//            }
//            referred.add(value.v)
//            current[key] = value.v
//        }
        val free = (x.variables - referred).toList()
        if (free.size < remains.size) {
            return emptyList()
        }
        val results = arrayListOf<FormulaResult>()
        fun recurMatch(i: Int, current: MutableMap<String, Term>) {
            if (i >= remains.size) {
                val f = required.build(varNames.map {
                    current[it]!!
                })
                if (x.isIdentityTo(f)) {
                    val nVarMap = hashMapOf<String, RefTerm>()
                    nVarMap.putAll(varMap)
                    for (en in current) {
                        nVarMap[en.key] = RefTerm(en.value)
                    }
                    val result = FormulaResult(formulaMap, nVarMap)
                    results += result
                }
                return
            }
            val r = remains[i]
            for (f in free) {
                current[r] = VarTerm(f)
                recurMatch(i + 1, current)
            }
        }
        recurMatch(0, reference)
        return results
    }

    private fun matchFree(
        x: Formula,
        formulaMap: FMap, varMap: TMap
    ): List<FormulaResult> {
        val (reference, remains) = buildReferredAndRemains(varMap)
        val referred = hashSetOf<Variable>()
        val current = hashMapOf<String, Variable>()
        reference.forEach { (key, value) ->
            require(value is VarTerm) {
                "Only variable reference is supported now."
            }
            referred.add(value.v)
            current[key] = value.v
        }

        val free = (x.variables - referred).toList()

        val choosen = BooleanArray(free.size) { false }

        val results = arrayListOf<FormulaResult>()
        fun recurMatch(i: Int) {
            if (i < remains.size) {
                val v = remains[i]
                for ((j, f) in free.withIndex()) {
                    if (choosen[j]) {
                        continue
                    }
                    current[v] = f
                    recurMatch(i + 1)
                }
                return
            }

            val nVarMap = hashMapOf<String, RefTerm>()
            nVarMap.putAll(varMap)
            for (en in current) {
                nVarMap[en.key] = RefTerm(VarTerm(en.value))
            }

            val namer = Variable.getXNNameProvider("x").iterator()
            val parameters = varNames.map<String, Variable> {
                val n = current[it]
                if (n != null) {
                    return@map n
                }
                //we need to find a proper name for the unreferenced variables
                var v = Variable(it)
                if (v !in x.allVariables) {
                    return@map v
                }
                while (true) {
                    v = namer.next()
                    if (v !in x.allVariables) {
                        break
                    }
                }
                v
            }
            val nFormulaMap = formulaMap + (name to RefFormula(x, varNames.size, parameters))
            val result = FormulaResult(nFormulaMap, nVarMap)
            results.addAll(sub.match(x, result))
        }
        recurMatch(0)
        return results
    }

    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        val (formulaMap, varMap) = previousResult.destructed
        val required = formulaMap[name]


        val r = if (required != null) {
            //already matched
            matchRequired(x, required, formulaMap, varMap)
        } else {
            matchFree(x, formulaMap, varMap)
        }
        return r

//        return if (required == null) {
//            listOf(FormulaResult(formulaMap + (name to x), varMap))
//        } else {
//            if (required.isIdentityTo(x)) {
//                listOf(FormulaResult(formulaMap, varMap))
//            } else {
//                emptyList()
//            }
//        }
    }
}

class NamedFormulaMatcher(
    val name: QualifiedName,
    val termMatchers: List<TermMatcher>,
    val ordered: Boolean = true
) :
    FormulaMatcher {
//    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
//        val (formulaMap, varMap) = previousResult.destructed
//        return if (x is NamedFormula && x.name == name) {
//            listOf(FormulaResult(formulaMap, varMap))
//        } else {
//            emptyList()
//        }
//    }

    override fun match(x: Formula, previousResult: FormulaResult?): List<FormulaResult> {
        val (formulaMap, varMap) = previousResult.destructed
        if (x !is NamedFormula || x.name != name) {
            return emptyList()
        }
        val results = if (ordered) {
            MatcherUtil.orderedMatch(
                x.parameters, termMatchers,
                TermMatchResult(varMap)
            )
        } else {
            MatcherUtil.unorderedMatch(
                x.parameters, termMatchers,
                TermMatchResult(varMap)
            )
        }
        return results.map { r -> FormulaResult(formulaMap, varMap + r.varMap) }
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
        val nVarMap = varMap + (varName to RefTerm(VarTerm(variable)))
        val res = sub.match(x.child, FormulaResult(formulaMap, nVarMap))
        return res.map { re ->
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