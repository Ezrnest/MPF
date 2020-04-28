package cn.ancono.mpf.builder

import cn.ancono.mpf.core.*
import cn.ancono.mpf.matcher.FMap
import cn.ancono.mpf.structure.IN_PREDICATE


/**
 * Builds a formula using an empty context.
 */
fun buildFormula(builderAction: SimpleFormulaContext.() -> Formula): Formula = builderAction(
    SimpleFormulaContext
)

/*
 * Created by liyicheng at 2020-04-05 17:09
 */
open class FormulaBuilderContext<T : TermBuilderContext>(
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

    fun named(name: QualifiedName, vararg parameters: Variable): NamedFormula {
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


    fun term(termBuilderAction: TermBuilderContext.() -> Term): Term {
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

    infix fun Term.belongTo(t: Term): Formula {
        return PredicateFormula(IN_PREDICATE, listOf(this, t))
    }

}


object SimpleFormulaContext : FormulaBuilderContext<SimpleTermContext>(
    SimpleTermContext
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
    val X = "X".v

    @JvmField
    val Y = "Y".v
}

class RefFormulaContext(val formulas: FMap, termContext: RefTermContext) :
    FormulaBuilderContext<RefTermContext>(termContext) {

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

    fun refNonNull(name: String): Formula {
        return formulas[name] ?: throw NoSuchElementException("No formula named `$name`")
    }


    fun hasRef(name: String): Boolean {
        return name in formulas
    }

    fun ref(name: String): Formula? {
        return formulas[name]
    }

    val String.fr: Formula
        get() = refNonNull(this)

    val String.tr: Term
        get() = termContext.termRef(this)

    fun unusedVar() : Term{
        return termContext.unusedVar()
    }
}