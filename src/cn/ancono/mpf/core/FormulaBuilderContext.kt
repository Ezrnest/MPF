package cn.ancono.mpf.core


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

    val P = +"P"
    val Q = +"Q"
    val R = +"R"


    operator fun String.unaryPlus(): Formula {
        return formulas[this] ?: NamedFormula(QualifiedName(this))
    }


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

    infix fun Formula.equalTo(g: Formula): Formula = EquivalentFormula(this, g)


    fun exist(variable: String, f: Formula): Formula = ExistFormula(f, Variable(variable))

    fun exist(variable: String, builderAction: FormulaBuilderContext.() -> Formula): Formula =
        exist(variable, builderAction(this))

    fun forAny(variable: String, f: Formula): Formula = ForAnyFormula(f, Variable(variable))

    fun forAny(variable: String, builderAction: FormulaBuilderContext.() -> Formula): Formula =
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


    companion object {
        val EMPTY_CONTEXT = FormulaBuilderContext()
    }
}

fun main() {
    val f = buildFormula {
        P
    }
}


