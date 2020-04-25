package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-04 14:29
 */

/**
 * Describes the variable in a logic formula.
 */
data class Variable(val name: String)


/**
 * Describes a first order logic formula.
 */
sealed class Formula : Node<Formula> {
    /**
     * Gets the free variables in this formula.
     */
    abstract val variables: Set<Variable>

    abstract override val childCount: Int

    /**
     * Gets all the variables appearing in this formula.
     */
    abstract val allVariables: Set<Variable>

    abstract fun isIdentityTo(other: Formula): Boolean

    /**
     * Performs a 'multi-branch' mapping recursively to this formula. Each formula in the returned list only differs
     * with the original formula by one formula node, which is one of the result of applying [f] to the formula node.
     *
     * For example, assume we have formula `A and B`, and the mapping function is `x -> !x`, then the result will
     * be `[!A and B, A and !B, !(A and B)]`
     */
    abstract fun recurMapMulti(f: (Formula) -> List<Formula>): List<Formula>

    abstract fun recurMap(f: (Formula) -> Formula): Formula

    /**
     * Flatten this formula so that all the combined nodes do not contain a children of the same type.
     */
    abstract fun flatten() : Formula
}

sealed class AtomicFormula : Formula(), AtomicNode<Formula> {
    override val childCount: Int
        get() = 0

    override fun recurMapMulti(f: (Formula) -> List<Formula>): List<Formula> {
        return f(this)
    }

    override fun recurMap(f: (Formula) -> Formula): Formula {
        return f(this)
    }

    override fun flatten(): Formula {
        return this
    }
}

/**
 * Describes a formula composed of a predicate, such as
 * ` a = b `, ` a < b `.
 */
class PredicateFormula(val p: Predicate, val terms: List<Term>) : AtomicFormula() {
    override val variables: Set<Variable> by lazy { terms.flatMapTo(hashSetOf()) { it.variables } }
    override val allVariables: Set<Variable>
        get() = variables

    override fun isIdentityTo(other: Formula): Boolean {
        if (this === other) return true
        if (other !is PredicateFormula) return false
        if (p != other.p) return false
        if (terms != other.terms) return false

        return true
    }

}

/**
 * Describes a formula with only a name and variables. This kind of formula is usually
 * an abbreviation of a complex formula. This formula is treated as atomic formula.
 */
class NamedFormula(val name: QualifiedName, override val variables: Set<Variable> = emptySet()) : AtomicFormula() {
    override val allVariables: Set<Variable>
        get() = variables

    override fun isIdentityTo(other: Formula): Boolean {
        if (other !is NamedFormula) {
            return false
        }
        return name == other.name
    }
}


sealed class CombinedFormula(override val children: List<Formula>) : Formula(), CombinedNode<Formula> {
    override val variables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.variables } }

    override val allVariables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.allVariables } }

    override fun isIdentityTo(other: Formula): Boolean {
        return other is CombinedFormula &&
                this.javaClass == other.javaClass &&
                children == other.children
    }

    override val childCount: Int
        get() = children.size


    abstract override fun copyOf(newChildren: List<Formula>): CombinedFormula

    override fun recurMapMulti(f: (Formula) -> List<Formula>): List<Formula> {
        val possibleChildren = children.map { recurMapMulti(f) }
        val result = ArrayList<Formula>(possibleChildren.sumBy { it.size })
        for (i in possibleChildren.indices) {
            for (c in possibleChildren[i]) {
                val newChildren = ArrayList(children)
                newChildren[i] = c
                result += copyOf(newChildren)
            }
        }
        result.addAll(f(this))
        return result
    }

    override fun recurMap(f: (Formula) -> Formula): Formula {
        return f(copyOf(children.map { it.recurMap(f) }))
    }



}


sealed class UnaryFormula(child: Formula) : CombinedFormula(listOf(child)) {
    val child: Formula
        get() = children[0]

    override fun flatten(): Formula {
        return copyOf(children.map(Formula::flatten))
    }
}

sealed class BinaryFormula(child1: Formula, child2: Formula) : CombinedFormula(listOf(child1, child2)) {
    val child1: Formula
        get() = children[0]

    val child2: Formula
        get() = children[1]

    override fun flatten(): Formula {
        return copyOf(children.map(Formula::flatten))
    }
}

sealed class QualifiedFormula(child: Formula, val v: Variable) :
    UnaryFormula(child) {
    override val variables: Set<Variable> by lazy {
        child.variables - v
    }


    override fun isIdentityTo(other: Formula): Boolean {
        return other is QualifiedFormula &&
                super.isIdentityTo(other) &&
                v == other.v
    }
}

class NotFormula(child: Formula) : UnaryFormula(child) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return NotFormula(newChildren[0])
    }
}

sealed class MultiFormula(children: List<Formula>) : CombinedFormula(children) {
    override fun flatten(): Formula {
        return if(childCount <= 1){
            children.first().flatten()
        }else{
            val newChildren = ArrayList<Formula>(childCount)
            for (c in children) {
                val nc = c.flatten()
                if (nc is MultiFormula && nc::class == this::class) {
                    newChildren.addAll(nc.children)
                }else{
                    newChildren.add(nc)
                }
            }
            copyOf(newChildren)
        }
    }
}

class AndFormula(children: List<Formula>) : MultiFormula(children) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return AndFormula(newChildren)
    }
}

class OrFormula(children: List<Formula>) : MultiFormula(children) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return OrFormula(newChildren)
    }
}

class ForAnyFormula(child: Formula, v: Variable) : QualifiedFormula(child, v) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return ForAnyFormula(newChildren[0], v)
    }
}

class ExistFormula(child: Formula, v: Variable) : QualifiedFormula(child, v) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return ExistFormula(newChildren[0], v)
    }
}

class ImplyFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return ImplyFormula(newChildren[0], newChildren[1])
    }
}

class EquivalentFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return EquivalentFormula(newChildren[0], newChildren[1])
    }
}

