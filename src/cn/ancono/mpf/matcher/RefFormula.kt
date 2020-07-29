package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.Formula
import cn.ancono.mpf.core.Term
import cn.ancono.mpf.core.VarTerm
import cn.ancono.mpf.core.Variable


/*
 * Created by liyicheng at 2020-04-28 15:51
 */
/**
 * Describes a referred formula in matching.
 * @author liyicheng
 */
class RefFormula(val formula: Formula, val varCount: Int = 0, val parameters: List<Variable> = emptyList()) {
//    val variables : Set<Variable> = parameters.toSet()

    val isClosed: Boolean
        get() = varCount == 0

    fun build(vars: List<Term>): Formula {
        require(vars.size == parameters.size)
        val map = hashMapOf<Variable, Term>()
        for ((v, nv) in parameters.zip(vars)) {
            map[v] = nv
        }
        return formula.replaceVar { map.getOrDefault(it, VarTerm(it)) }
    }

    fun build(varMap: TMap): Formula {
        if (isClosed) {
            return formula
        }
        val vars = parameters.map { v -> varMap[v.name]?.term ?: v as Term }
        return build(vars)
//        return formula.replaceVar {v ->
//            val ref = varMap[v.name] ?: return@replaceVar VarTerm(v)
//            ref.term
//        }
    }

    override fun toString(): String {
        if (varCount == 0) {
            return formula.toString()
        } else {
            return parameters.joinToString(",", prefix = "(", postfix = ")") { it.name } + "|->" + formula
        }
    }
}

/**
 * Describes a referred term in matching.
 * @author liyicheng
 */
class RefTerm(val term: Term, val varCount: Int = 0, val parameters: List<Variable> = emptyList()) {
    val isClosed: Boolean
        get() = varCount == 0

    fun build(vars: List<Term>): Term {
        require(vars.size == parameters.size)
        val map = parameters.zip(vars).toMap()

        return term.replaceVar(map)
    }

    fun build(varMap: TMap): Term {
        if (isClosed) {
            return term
        }
        val vars = parameters.map { v -> varMap[v.name]?.term ?: v as Term }
        return build(vars)
//        return term.replaceVar { v ->
//            val ref = varMap[v.name] ?: return@replaceVar VarTerm(v)
//            ref.term
//        }
    }

    override fun toString(): String {
        if (varCount == 0) {
            return term.toString()
        } else {
            return parameters.joinToString(",", prefix = "(", postfix = ")") { it.name } + "|->" + term
        }
    }
}