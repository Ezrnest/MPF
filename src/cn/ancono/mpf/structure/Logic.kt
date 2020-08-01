package cn.ancono.mpf.structure

import cn.ancono.mpf.core.*
import cn.ancono.mpf.core.Function


/**
 * The basic first order logic structure.
 */
object LogicStructure : Structure {
    override val name = "Logic"


    override val predicateMap: Map<QualifiedName, Predicate> = mapOf(
        EQUAL_PREDICATE.name to EQUAL_PREDICATE
    )
    override val functionMap: Map<QualifiedName, Function> = emptyMap()
    override val constantMap: Map<QualifiedName, Constant> = emptyMap()
    override val ruleMap: Map<QualifiedName, Rule> = LogicRules.rulesAsMap()
    override val defaultRules: Map<QualifiedName, Rule> = mapOf(
        LogicRules.AllLogicRule.name to LogicRules.AllLogicRule
    )
}