package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.*
import cn.ancono.mpf.structure.IN_PREDICATE


fun buildMatcher(builderAction: FormulaMatcherContext.() -> FormulaMatcher): FormulaMatcher =
    builderAction(FormulaMatcherContext)

/*
 * Created by liyicheng at 2020-04-25 17:54
 */
object FormulaMatcherContext {
    val String.ref
        get() = RefFormulaMatcher(this)

    val String.named
        get() = NamedFormulaMatcher(QualifiedName(this))


    /**
     * Reference of variable
     */
    val String.rv
        get() = RefTermMatcher(this)
    /**
     * Variable named as `this`.
     */
    val String.v
        get() = VarTermMatcher(Variable(this))

    val String.c
        get() = ConstTermMatcher(Constance(QualifiedName(this)))

    @JvmField
    val P = "P".ref

    @JvmField
    val Q = "Q".ref

    @JvmField
    val R = "R".ref

    @JvmField
    val a = "a".rv

    @JvmField
    val b = "b".rv

    @JvmField
    val c = "c".rv

    @JvmField
    val A = "A".rv

    @JvmField
    val B = "B".rv

    @JvmField
    val C = "C".rv

    @JvmField
    val x = "x".rv

    @JvmField
    val y = "y".rv

    @JvmField
    val X = "X".rv

    @JvmField
    val Y = "Y".rv

    infix fun FormulaMatcher.and(m: FormulaMatcher): FormulaMatcher = AndFormulaMatcher(listOf(this, m), EmptyMatcher)
    infix fun FormulaMatcher.or(m: FormulaMatcher): FormulaMatcher = OrFormulaMatcher(listOf(this, m), EmptyMatcher)
    infix fun FormulaMatcher.implies(m: FormulaMatcher): FormulaMatcher = ImplyFormulaMatcher(this, m)
    infix fun FormulaMatcher.equivTo(m: FormulaMatcher): FormulaMatcher = EquivalentFormulaMatcher(this, m)
    operator fun FormulaMatcher.not(): FormulaMatcher = NotFormulaMatcher(this)

    class PredicateFunction(val name: QualifiedName) {
        operator fun invoke(vararg terms: TermMatcher): FormulaMatcher {
            val predicate = Predicate(terms.size, name)
            return PredicateFormulaMatcher(predicate, terms.asList())
        }
    }

    val String.r: PredicateFunction
        get() = PredicateFunction(QualifiedName(this))

    infix fun TermMatcher.equalTo(t: TermMatcher): FormulaMatcher {
        return PredicateFormulaMatcher(EQUAL_PREDICATE, listOf(this, t), false)
    }

    infix fun TermMatcher.belongTo(t: TermMatcher): FormulaMatcher {
        return PredicateFormulaMatcher(IN_PREDICATE, listOf(this, t), true)
    }

    fun exist(variable: String, f: FormulaMatcher): FormulaMatcher = ExistFormulaMatcher(variable, f)

    fun exist(variable: String, builderAction: FormulaMatcherContext.() -> FormulaMatcher): FormulaMatcher =
        exist(variable, builderAction(this))


    fun exist(variable: RefTermMatcher, f: FormulaMatcher): FormulaMatcher = ExistFormulaMatcher(variable.refName, f)

    fun exist(variable: RefTermMatcher, builderAction: FormulaMatcherContext.() -> FormulaMatcher): FormulaMatcher =
        exist(variable, builderAction(this))

    fun forAny(variable: String, f: FormulaMatcher): FormulaMatcher = ForAnyFormulaMatcher(variable, f)

    fun forAny(variable: String, builderAction: FormulaMatcherContext.() -> FormulaMatcher): FormulaMatcher =
        forAny(variable, builderAction(this))

    fun forAny(variable: RefTermMatcher, f: FormulaMatcher): FormulaMatcher = ForAnyFormulaMatcher(variable.refName, f)

    fun forAny(variable: RefTermMatcher, builderAction: FormulaMatcherContext.() -> FormulaMatcher): FormulaMatcher =
        forAny(variable, builderAction(this))

}