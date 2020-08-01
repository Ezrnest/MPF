package cn.ancono.mpf.core

import cn.ancono.mpf.core.QualifiedName.Companion.of
import java.lang.IllegalArgumentException


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
interface Structure {

    val name: String
    val predicateMap: Map<QualifiedName, Predicate>
    val functionMap: Map<QualifiedName, Function>
    val constantMap: Map<QualifiedName, Constant>
    val ruleMap: Map<QualifiedName, Rule>
    val defaultRules: Map<QualifiedName, Rule>

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

interface MutableStructure : Structure {
    fun addPredicate(predicate: Predicate): MutableStructure

    fun addFunction(function: Function): MutableStructure

    fun addConstant(constant: Constant): MutableStructure

    fun addRule(rule: Rule): MutableStructure

    fun addDefaultRule(rule: Rule): MutableStructure

    fun copy(): MutableStructure

    companion object{
        fun of(structure: Structure) : MutableStructure{
            return MutableStructureImpl(structure)
        }

    }
}


private class MutableStructureImpl(
    override val name: String,
    predicateMap: Map<QualifiedName, Predicate>,
    functionMap: Map<QualifiedName, Function>,
    constantMap: Map<QualifiedName, Constant>,
    ruleMap: Map<QualifiedName, Rule>,
    defaultRules: Map<QualifiedName, Rule>
) : MutableStructure {

    constructor(structure: Structure) : this(
        structure.name,
        structure.predicateMap,
        structure.functionMap,
        structure.constantMap,
        structure.ruleMap,
        structure.defaultRules
    )

    private val mPredicateMap: MutableMap<QualifiedName, Predicate> = predicateMap.toMutableMap()
    private val mFunctionMap: MutableMap<QualifiedName, Function> = functionMap.toMutableMap()
    private val mConstantMap: MutableMap<QualifiedName, Constant> = constantMap.toMutableMap()
    private val mRuleMap: MutableMap<QualifiedName, Rule> = ruleMap.toMutableMap()
    private val mDefaultRules: MutableMap<QualifiedName, Rule> = defaultRules.toMutableMap()


    override val predicateMap: Map<QualifiedName, Predicate>
        get() = mPredicateMap
    override val functionMap: Map<QualifiedName, Function>
        get() = mFunctionMap
    override val constantMap: Map<QualifiedName, Constant>
        get() = mConstantMap
    override val ruleMap: Map<QualifiedName, Rule>
        get() = mRuleMap
    override val defaultRules: Map<QualifiedName, Rule>
        get() = mDefaultRules


    private fun <K, V> addToMapNoDuplication(m: MutableMap<K, V>, k: K, v: V) {
        if (m.containsKey(k)) {
            throw DuplicatedNameException("Duplicated name: $k")
        }
        m[k] = v
    }

    override fun addPredicate(predicate: Predicate): MutableStructure {
        addToMapNoDuplication(mPredicateMap, predicate.name, predicate)
        return this
    }

    override fun addFunction(function: Function): MutableStructure {
        addToMapNoDuplication(mFunctionMap, function.name, function)
        return this
    }

    override fun addConstant(constant: Constant): MutableStructure {
        addToMapNoDuplication(mConstantMap, constant.name, constant)
        return this
    }

    override fun addRule(rule: Rule): MutableStructure {
        addToMapNoDuplication(mRuleMap, rule.name, rule)
        return this
    }

    override fun addDefaultRule(rule: Rule): MutableStructure {
        addToMapNoDuplication(mDefaultRules, rule.name, rule)
        return this
    }

    override fun copy(): MutableStructure {
        return OnAddCopyStructure(this)
    }


}

private class OnAddCopyStructure(private val base: Structure) : MutableStructure {
    override val name: String
        get() = base.name
    override val predicateMap: Map<QualifiedName, Predicate>
        get() = base.predicateMap
    override val functionMap: Map<QualifiedName, Function>
        get() = base.functionMap
    override val constantMap: Map<QualifiedName, Constant>
        get() = base.constantMap
    override val ruleMap: Map<QualifiedName, Rule>
        get() = base.ruleMap
    override val defaultRules: Map<QualifiedName, Rule>
        get() = base.defaultRules

    override fun addPredicate(predicate: Predicate): MutableStructure {
        return MutableStructureImpl(this).addPredicate(predicate)
    }

    override fun addFunction(function: Function): MutableStructure {
        return MutableStructureImpl(this).addFunction(function)
    }

    override fun addConstant(constant: Constant): MutableStructure {
        return MutableStructureImpl(this).addConstant(constant)
    }

    override fun addRule(rule: Rule): MutableStructure {
        return MutableStructureImpl(this).addRule(rule)
    }

    override fun addDefaultRule(rule: Rule): MutableStructure {
        return MutableStructureImpl(this).addDefaultRule(rule)
    }

    override fun copy(): MutableStructure {
        return OnAddCopyStructure(this)
    }
}