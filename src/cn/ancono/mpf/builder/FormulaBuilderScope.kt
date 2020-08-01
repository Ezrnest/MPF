package cn.ancono.mpf.builder

import cn.ancono.mpf.core.*
import cn.ancono.mpf.matcher.*
import cn.ancono.mpf.structure.IN_PREDICATE


/**
 * Builds a formula using an empty context.
 */
fun buildFormula(builderAction: SimpleFormulaScope.() -> Formula): Formula = builderAction(
    SimpleFormulaScope
)

/*
 * Created by liyicheng at 2020-04-05 17:09
 */
open class FormulaBuilderScope<T : TermBuilderScope>(
    val termContext: T
) {

    val a = "a".v
        @JvmName("geta")
        get

    val b = "b".v
        @JvmName("getb")
        get
    val c = "c".v
        @JvmName("getc")
        get

    @JvmField
    val A = "A".v

    @JvmField
    val B = "B".v

    @JvmField
    val C = "C".v

    val String.n: NamedFormula
        get() = named(QualifiedName(this))

    fun String.n(vararg terms: Term): NamedFormula {
        return named(QualifiedName(this), *terms)
    }

    fun named(name: QualifiedName, vararg parameters: Term): NamedFormula {
        return NamedFormula(name, parameters.toList())
    }

    /**
     * Gets a named formula or a predefined formula.
     */
    operator fun String.unaryPlus(): Formula {
        return named(QualifiedName(this))
    }


    /**
     * Gets a variable term or a
     */
    val String.v: Term
        get() = termContext.variable(this)


    infix fun Formula?.and(g: Formula?): Formula {
        if (this == null) {
            return g!!
        } else if (g == null) {
            return this
        }
        return when (this) {
            is AndFormula -> {
                when (g) {
                    is AndFormula -> AndFormula(
                        this.children + g.children
                    )
                    else -> AndFormula(this.children + g)
                }
            }
            else -> {
                when (g) {
                    is AndFormula -> AndFormula(
                        g.children + this
                    )
                    else -> AndFormula(listOf(this, g))
                }
            }
        }
    }

    infix fun Formula?.or(g: Formula?): Formula {
        if (this == null) {
            return g!!
        } else if (g == null) {
            return this
        }
        return when (this) {
            is OrFormula -> {
                when (g) {
                    is OrFormula -> OrFormula(
                        this.children + g.children
                    )
                    else -> OrFormula(this.children + g)
                }
            }
            else -> {
                when (g) {
                    is OrFormula -> OrFormula(
                        g.children + this
                    )
                    else -> OrFormula(listOf(this, g))
                }
            }
        }
    }

    operator fun Formula.not() = when (this) {
        is NotFormula -> this.child
        else -> NotFormula(this)
    }

    infix fun Formula.implies(g: Formula): Formula =
        ImplyFormula(this, g)

    infix fun Formula.equivTo(g: Formula): Formula =
        EquivalentFormula(this, g)


    fun exist(variable: String, f: Formula): Formula =
        ExistFormula(f, Variable(variable))

    @Suppress("UNCHECKED_CAST")
    fun exist(variable: String, builderAction: () -> Formula): Formula =
        exist(variable, builderAction())

    fun forAny(variable: String, f: Formula): Formula =
        ForAnyFormula(f, Variable(variable))

    @Suppress("UNCHECKED_CAST")
    fun forAny(variable: String, builderAction: () -> Formula): Formula =
        forAny(variable, builderAction())


    fun exist(variable: Term, f: Formula): Formula {
        require(variable is VarTerm)
        return ExistFormula(f, variable.v)
    }

    @Suppress("UNCHECKED_CAST")
    fun exist(variable: Term, builderAction: () -> Formula): Formula =
        exist(variable, builderAction())

    fun forAny(variable: Term, f: Formula): Formula {
        require(variable is VarTerm)
        return ForAnyFormula(f, variable.v)
    }

    @Suppress("UNCHECKED_CAST")
    fun forAny(variable: Term, builderAction: () -> Formula): Formula =
        forAny(variable, builderAction())


    fun term(termBuilderAction: TermBuilderScope.() -> Term): Term {
        return termBuilderAction(termContext)
    }

    class PredicateFunction(val name: QualifiedName) {

        operator fun invoke(vararg terms: Term): Formula {
            val predicate = Predicate(terms.size, name)
            return PredicateFormula(predicate, terms.asList())
        }
    }

    /**
     * Builds a predicate formula whose predicate's name is `this` string.
     *
     * Example:
     * ` "contains".r(A,B) `
     */
    val String.p: PredicateFunction
        get() = PredicateFunction(
            QualifiedName(
                this
            )
        )

    infix fun Term.equalTo(t: Term): Formula {
        return PredicateFormula(
            EQUAL_PREDICATE,
            listOf(this, t)
        )
    }




}


object SimpleFormulaScope : FormulaBuilderScope<SimpleTermScope>(
    SimpleTermScope
) {

    @JvmField
    val P = +"P"

    @JvmField
    val Q = +"Q"

    @JvmField
    val R = +"R"

    @JvmField
    val x = "x".v

    @JvmField
    val y = "y".v

    @JvmField
    val z = "z".v

    @JvmField
    val X = "X".v

    @JvmField
    val Y = "Y".v

}

open class RefFormulaScope(val formulas: FMap, termContext: RefTermScope) :
    FormulaBuilderScope<RefTermScope>(termContext) {

    val x
        @JvmName("getx")
        get() = "x".tr

    val y
        @JvmName("gety")
        get() = "y".tr

    val X
        @JvmName("getX")
        get() = "X".tr
    val Y
        @JvmName("getY")
        get() = "Y".tr

    val P
        get() = "P".fr
    val Q
        get() = "Q".fr
    val R
        get() = "R".fr

    val usedVariables : Set<Variable>
    init{
        val vars = hashSetOf<Variable>()
        for (rf in formulas.values) {
            vars.addAll(rf.parameters)
            vars.addAll(rf.formula.allVariables)
        }
        vars.addAll(termContext.usedVariables)
        usedVariables = vars
    }

    fun refNonNull(name: String): Formula {
        val f = formulas[name] ?: throw NoSuchElementException("No formula named `$name`")
        return f.build(termContext.context)
    }


    fun hasRef(name: String): Boolean {
        return name in formulas
    }

    fun ref(name: String): Formula? {
        return formulas[name]?.build(termContext.context)
    }

    /**
     * Formula reference
     */
    val String.fr: Formula
        get() = refNonNull(this)

    /**
     * Term reference
     */
    val String.tr: Term
        get() = termContext.termRef(this)

    fun unusedVar(): Variable {
        return Variable.getXNNameProvider().first { it !in usedVariables }
    }

    fun unusedVars() : Sequence<Variable>{
        return Variable.getXNNameProvider().filter { it !in usedVariables }
    }

    inner class RefMatcherFunction(val name: String) {
        operator fun invoke(vararg terms: Term): Formula {
            val f = this@RefFormulaScope.formulas[name] ?: throw NoSuchElementException("No formula named `$name`")
            return f.build(terms.toList())
//            return VarRefFormulaMatcher(name, terms.map { (it as RefTermMatcher).refName })
        }
    }

    val phi = RefMatcherFunction("phi")

    val rho = RefMatcherFunction("rho")

    val psi = RefMatcherFunction("psi")
}