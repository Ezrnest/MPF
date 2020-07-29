package cn.ancono.mpf.core

import cn.ancono.mpf.core.QualifiedName.Companion.of


/*
 * Created by liyicheng at 2020-04-04 19:27
 */
/**
 * Describes a predicate in a structure.
 * @author liyicheng
 */
class Predicate(
    val argLength: Int,
    val name: QualifiedName,
    val ordered: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Predicate

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

/**
 * Describes a function in a structure.
 * @author liyicheng
 */
class Function(
    /**
     * The length of arguments of this function, -1 means the length of arguments is variable..
     */
    val argLength: Int,
    val name: QualifiedName,
    val ordered: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Function

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}

data class Constant(val name: QualifiedName)

/**
 * Describes a structure of first order logic.
 */
open class Structure(
    val name: String,
    val predicateMap: Map<QualifiedName, Predicate>,
    val functionMap: Map<QualifiedName, Function>,
    val constantMap: Map<QualifiedName, Constant>,
    val ruleMap: Map<QualifiedName, Rule>,
    val defaultRules: Map<QualifiedName, Rule>
) {


    val predicates
        get() = predicateMap.values


    val functions
        get() = functionMap.values


    val constants
        get() = constantMap.values


    val rules
        get() = ruleMap.values


}

val EQUAL_PREDICATE = Predicate(2, of("equals", "logic"), false)


/**
 * The basic first order logic structure.
 */
object LogicStructure : Structure(
    "Logic",
    mapOf(
        EQUAL_PREDICATE.name to EQUAL_PREDICATE
    ),
    emptyMap(),
    emptyMap(),
    LogicRules.rulesAsMap(),
    mapOf(
        LogicRules.AllLogicRule.name to LogicRules.AllLogicRule
    )
)

