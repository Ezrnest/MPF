package cn.ancono.mpf.core




/**
 * Describes a term in the
 * Created by liyicheng at 2020-04-04 18:53
 */
sealed class Term {
    abstract val variables: Set<Variable>
}

class VarTerm(val v: Variable) : Term() {
    override val variables: Set<Variable> = setOf(v)
}

class FunTerm(val f: Function, val args: List<Term>) : Term() {
    override val variables: Set<Variable> by lazy { args.flatMapTo(hashSetOf()) { it.variables } }
}

class ConstTerm(val c: Contance) : Term() {
    override val variables: Set<Variable>
        get() = emptySet()
}

class NamedTerm(val name: QualifiedName, override val variables: Set<Variable>) : Term()