package cn.ancono.mpf.core

import cn.ancono.mpf.matcher.FMap
import cn.ancono.mpf.structure.IN_PREDICATE


fun buildFormula(builderAction: FormulaBuilderContext.() -> Formula): Formula = builderAction(
    FormulaBuilderContext.EMPTY_CONTEXT
)

/*
 * Created by liyicheng at 2020-04-05 17:09
 */
open class FormulaBuilderContext(
    val formulas: FMap = emptyMap(),
    val termContext: TermBuilderContext = TermBuilderContext.EMPTY_CONTEXT
) {
    @JvmField
    val P = +"P"
    @JvmField
    val Q = +"Q"
    @JvmField
    val R = +"R"
    @JvmField
    val a = "a".v
    @JvmField
    val b = "b".v
    @JvmField
    val c = "c".v
    @JvmField
    val A = "A".v
    @JvmField
    val B = "B".v
    @JvmField
    val C = "C".v
    @JvmField
    val x = "x".v
    @JvmField
    val y = "y".v

    @JvmField
    val X = "X".v
    @JvmField
    val Y = "Y".v

    operator fun String.unaryPlus(): Formula {
        return formulas[this] ?: NamedFormula(QualifiedName(this))
    }

    val String.v: Term
        get() = termContext.variable(this)


    infix fun Formula.and(g: Formula): Formula = when (this) {
        is AndFormula -> {
            when (g) {
                is AndFormula -> AndFormula(this.children + g.children)
                else -> AndFormula(this.children + g)
            }
        }
        else -> {
            when (g) {
                is AndFormula -> AndFormula(g.children + this)
                else -> AndFormula(listOf(this, g))
            }
        }
    }

    infix fun Formula.or(g: Formula): Formula = when (this) {
        is OrFormula -> {
            when (g) {
                is OrFormula -> OrFormula(this.children + g.children)
                else -> OrFormula(this.children + g)
            }
        }
        else -> {
            when (g) {
                is OrFormula -> OrFormula(g.children + this)
                else -> OrFormula(listOf(this, g))
            }
        }
    }

    operator fun Formula.not() = when (this) {
        is NotFormula -> this.child
        else -> NotFormula(this)
    }

    infix fun Formula.implies(g: Formula): Formula = ImplyFormula(this, g)

    infix fun Formula.equivTo(g: Formula): Formula = EquivalentFormula(this, g)


    fun exist(variable: String, f: Formula): Formula = ExistFormula(f, Variable(variable))

    fun exist(variable: String, builderAction: FormulaBuilderContext.() -> Formula): Formula =
        exist(variable, builderAction(this))

    fun forAny(variable: String, f: Formula): Formula = ForAnyFormula(f, Variable(variable))

    fun forAny(variable: String, builderAction: FormulaBuilderContext.() -> Formula): Formula =
        forAny(variable, builderAction(this))


    fun exist(variable: Term, f: Formula): Formula{
        require(variable is VarTerm)
        return ExistFormula(f, variable.v)
    }

    fun exist(variable: Term, builderAction: FormulaBuilderContext.() -> Formula): Formula =
        exist(variable, builderAction(this))

    fun forAny(variable: Term, f: Formula): Formula {
        require(variable is VarTerm)
        return ForAnyFormula(f, variable.v)
    }

    fun forAny(variable: Term, builderAction: FormulaBuilderContext.() -> Formula): Formula =
        forAny(variable, builderAction(this))


    fun term(termBuilderAction: TermBuilderContext.() -> Term): Term {
        return termBuilderAction(termContext)
    }

    class PredicateFunction(val name: QualifiedName) {

        operator fun invoke(vararg terms: Term): Formula {
            val predicate = Predicate(terms.size, name)
            return PredicateFormula(predicate, terms.asList())
        }
    }

    val String.r: PredicateFunction
        get() = PredicateFunction(QualifiedName(this))

    infix fun Term.equalTo(t: Term): Formula {
        return PredicateFormula(EQUAL_PREDICATE, listOf(this, t))
    }

    infix fun Term.belongTo(t : Term) : Formula{
        return PredicateFormula(IN_PREDICATE, listOf(this, t))
    }

    companion object {
        val EMPTY_CONTEXT = FormulaBuilderContext()
    }
}



