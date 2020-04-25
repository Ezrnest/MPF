package cn.ancono.mpf.core


/**
 * Describes a term in the
 * Created by liyicheng at 2020-04-04 18:53
 */
interface Term : Node<Term> {
    val variables: Set<Variable>

    override val childCount: Int


    fun isIdentityTo(t: Term): Boolean

    /**
     * Applies the function recursively to each term nodes in this term. The order of
     * iteration is pre-order.
     */
    fun recurApply(f: (Term) -> Unit): Unit

    /**
     * Applies the mapping function recursively to each term nodes in this term to build a new term.
     *
     *
     * @param m a mapping function that takes the original term and the term whose sub-terms are mapped
     * as a parameter. If the term has no sub-term, then the two parameters will be the same.
     *
     */
    fun recurMap(m: (origin: Term, mapped: Term) -> Term): Term

    /**
     * Recursively maps the term and all it sub-terms and build a new term. The function [before] will be
     * invoked first before mapping the sub-nodes, and the function [after] will be invoked to a term with all sub-terms
     * mapped. If [before] returns a non-null value, then [after] will not be invoked for the term.
     *
     *
     *
     *
     */
    fun recurMap(before: (Term) -> Term?, after: (Term) -> Term): Term
}

abstract class AtomicTerm : Term, AtomicNode<Term> {

    override val childCount: Int
        get() = 0

    override val children: List<Term>
        get() = emptyList()

    override fun recurApply(f: (Term) -> Unit) {
        f(this)
    }

    override fun recurMap(m: (origin: Term, mapped: Term) -> Term): Term {
        return m(this, this)
    }

    override fun recurMap(before: (Term) -> Term?, after: (Term) -> Term): Term {
        return before(this) ?: after(this)
    }
}

/**
 * A term consists of only a variable.
 */
class VarTerm(val v: Variable) : AtomicTerm() {
    override val variables: Set<Variable> = setOf(v)

    override fun isIdentityTo(t: Term): Boolean {
        return t is VarTerm && v == t.v
    }
}

class ConstTerm(val c: Contance) : AtomicTerm() {
    override val variables: Set<Variable>
        get() = emptySet()

    override fun isIdentityTo(t: Term): Boolean {
        return t is ConstTerm && c == t.c
    }
}

class NamedTerm(val name: QualifiedName, override val variables: Set<Variable> = emptySet()) : AtomicTerm() {
    override fun isIdentityTo(t: Term): Boolean {
        return t is NamedTerm && name == t.name
    }
}

abstract class CombinedTerm(override val children: List<Term>) : Term, CombinedNode<Term> {
    override val childCount: Int
        get() = children.size

    abstract override fun copyOf(newChildren: List<Term>): CombinedTerm
}

/**
 * A function term, such as `f(a,b)`, `a + b`.
 */
class FunTerm(val f: Function, args: List<Term>) : CombinedTerm(args) {
    override val variables: Set<Variable> by lazy { args.flatMapTo(hashSetOf()) { it.variables } }

    override fun isIdentityTo(t: Term): Boolean {
        return t is FunTerm && f == t.f && children == t.children
    }

    override fun recurApply(f: (Term) -> Unit) {
        f(this)
        children.forEach { it.recurApply(f) }
    }

    override fun recurMap(m: (origin: Term, mapped: Term) -> Term): Term {
        val nArgs = children.map { it.recurMap(m) }
        val nTerm = FunTerm(f, nArgs)
        return m(this, nTerm)
    }

    override fun recurMap(before: (Term) -> Term?, after: (Term) -> Term): Term {
        val t = before(this)
        if (t != null) {
            return t
        }
        val nArgs = children.map { it.recurMap(before, after) }
        val nTerm = FunTerm(f, nArgs)
        return after(nTerm)
    }

    override fun copyOf(newChildren: List<Term>): CombinedTerm {
        return FunTerm(f, newChildren)
    }
}
