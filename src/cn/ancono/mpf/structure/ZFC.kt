package cn.ancono.mpf.structure

import cn.ancono.mpf.builder.FormulaBuilderScope
import cn.ancono.mpf.builder.TermBuilderScope
import cn.ancono.mpf.core.*
import cn.ancono.mpf.core.Function


/*
 * Created by liyicheng at 2020-04-26 15:49
 */
val IN_PREDICATE = Predicate(2, ZFC.nameOf("in"), true)

val EMPTY_SET = Constant(ZFC.nameOf("empty"))

object ZFCTermScope : TermBuilderScope() {
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

}

object ZFCFormulaScope : FormulaBuilderScope<ZFCTermScope>(ZFCTermScope) {

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


    infix fun Term.belongTo(t: Term): Formula {
        return PredicateFormula(IN_PREDICATE, listOf(this, t))
    }

    infix fun Term.contains(t : Term) : Formula{
        return PredicateFormula(ZFC.CONTAINS_PREDICATE, listOf(this,t))
    }

}

object ZFC : Structure {

    override val name: String = "ZFC"

    val namespace = name.toLowerCase()

    internal fun nameOf(name: String): QualifiedName {
        return QualifiedName.of(name, namespace)
    }


    val CONTAINS_PREDICATE = Predicate(2, nameOf("contains"))

    val RuleExtension = def({ A equalTo B }, {
        forAny(x) {
            (x belongTo A) equivTo (x belongTo B)
        }
    }, "RuleExtension", "A=B <=> any[x](x in A <-> x in B)")

    override val predicateMap: Map<QualifiedName, Predicate> = LinkedHashMap<QualifiedName, Predicate>().apply {
        putAll(LogicStructure.predicateMap)
        put(IN_PREDICATE.name, IN_PREDICATE)
    }

    override val functionMap: Map<QualifiedName, Function> = LogicStructure.functionMap
    // TODO add union, intersect, power to be functions

    override val constantMap: Map<QualifiedName, Constant> = LinkedHashMap<QualifiedName, Constant>().apply {
        putAll(LogicStructure.constantMap)
        put(EMPTY_SET.name, EMPTY_SET)
    }
    override val ruleMap: Map<QualifiedName, Rule> = LinkedHashMap<QualifiedName, Rule>().apply {
        putAll(LogicStructure.ruleMap)
        put(RuleExtension.name, RuleExtension)

    }
    override val defaultRules: Map<QualifiedName, Rule> = LinkedHashMap<QualifiedName, Rule>().apply {
        putAll(LogicStructure.defaultRules)

    }

    fun buildFormula(builderAction: ZFCFormulaScope.() -> Formula): Formula = builderAction(
        ZFCFormulaScope
    )


    private fun def(
        p: ZFCFormulaScope.() -> Formula,
        q: ZFCFormulaScope.() -> Formula,
        name: String, description: String = "None"
    ): Rule {
        val f1 = buildFormula(p)
        val f2 = buildFormula(q)

        return MatcherEquivRule.fromFormulas(nameOf(name), f1, f2, description)
    }



}