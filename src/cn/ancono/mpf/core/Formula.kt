package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-04 14:29
 */

/**
 * Describes the variable in a logic formula.
 */
data class Variable(val name: String) {
//    override fun toString(): String {
//        return name
//    }

    companion object {
        /**
         * Returns a name provider that returns "x1", "x2" ...
         */
        fun getXNNameProvider(leadingChar: String = "x"): Sequence<Variable> = sequence {
            var n = 1
            while (true) {
                yield(Variable("$leadingChar$n"))
                n++
            }
        }
    }
}


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
    abstract fun flatten(): Formula

    /**
     * The priority of the formula, 0 is the top.
     */
    protected abstract val bracketLevel: Int

    internal fun wrapBracket(f: Formula): String = if (f.bracketLevel >= this.bracketLevel) {
        "($f)"
    } else {
        f.toString()
    }

    /**
     * Renames all the variables in this formula according to the map. This method may
     * change the semantic meaning of the formula.
     */
    abstract fun renameAllVar(renamer: (Variable)->Variable): Formula

    open fun renameAllVar(nameMap: Map<Variable, Variable>): Formula = renameAllVar { nameMap.getOrDefault(it,it) }

    /**
     * Renames only the free variables in this formula according to the map.
     */
    abstract fun renameVar(renamer: (Variable)->Variable) : Formula

    open fun renameVar(nameMap: Map<Variable, Variable>): Formula = renameVar { nameMap.getOrDefault(it,it) }

    /**
     * Renames the variables in this formula. The [nameProvider] should provide a sequence of non-duplicate names.
     */
    fun regularizeVarName(nameProvider: Iterator<Variable> = Variable.getXNNameProvider().iterator()): Formula {
        return regularizeVarName(mutableMapOf(), nameProvider)
    }

    internal abstract fun regularizeVarName(
        nameMap: MutableMap<Variable, Variable>,
        nameProvider: Iterator<Variable>
    ): Formula


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

    override val bracketLevel: Int
        get() = 0


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

        return Utils.collectionEquals(this.terms,other.terms,Term::isIdentityTo)
    }

    override fun toString(): String {
        return p.name.displayName + terms.joinToString(",", prefix = "(", postfix = ")")
    }

    override fun renameAllVar(renamer: (Variable) -> Variable): Formula {
        return PredicateFormula(p, terms.map { it.renameVar(renamer) })
    }

    override fun renameVar(renamer: (Variable) -> Variable): Formula {
        return renameAllVar(renamer)
    }

    override fun regularizeVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        val newTerms = terms.map { it.regularizeVarName(nameMap, nameProvider) }
        return PredicateFormula(p, newTerms)
    }
}

/**
 * Describes a formula with only a name and variables. This kind of formula is usually
 * an abbreviation of a complex formula. This formula is treated as atomic formula.
 */
class NamedFormula(val name: QualifiedName, val parameters: List<Variable> = emptyList()) : AtomicFormula() {
    override val variables: Set<Variable> = parameters.toSet()
    override val allVariables: Set<Variable>
        get() = variables

    override fun isIdentityTo(other: Formula): Boolean {
        if (other !is NamedFormula) {
            return false
        }
        return name == other.name && parameters == other.parameters
    }

    override fun toString(): String {
        if (parameters.isEmpty()) {
            return name.displayName
        }
        return name.displayName + parameters.joinToString(",", prefix = "(", postfix = ")") { it.name }
    }

    override fun renameAllVar(nameMap: Map<Variable, Variable>): Formula {
        if (allVariables.any { it in nameMap }) {
            return NamedFormula(name, parameters.map { nameMap.getOrDefault(it, it) })
        }
        return this
    }

    override fun renameVar(nameMap: Map<Variable, Variable>): Formula {
        return renameAllVar(nameMap)
    }

    override fun renameAllVar(renamer: (Variable) -> Variable): Formula {
        return NamedFormula(name, parameters.map(renamer))
    }

    override fun renameVar(renamer: (Variable) -> Variable): Formula {
        return renameAllVar(renamer)
    }

    override fun regularizeVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        val nParameters = parameters.map<Variable,Variable> { v ->
            var nv = nameMap[v]
            if (nv == null) {
                nv = nameProvider.next()
                nameMap[v] = nv
            }
            nv
        }
        return NamedFormula(name, nParameters)
    }
}


sealed class CombinedFormula(override val children: List<Formula>) : Formula(), CombinedNode<Formula> {
    override val variables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.variables } }

    override val allVariables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.allVariables } }

    override fun isIdentityTo(other: Formula): Boolean {
        return other is CombinedFormula &&
                this.javaClass == other.javaClass &&
                Utils.collectionEquals(this.children,other.children,Formula::isIdentityTo)
    }

    override val childCount: Int
        get() = children.size


    abstract override fun copyOf(newChildren: List<Formula>): CombinedFormula

    override fun recurMapMulti(f: (Formula) -> List<Formula>): List<Formula> {
        val possibleChildren = children.map { it.recurMapMulti(f) }
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

    override fun renameAllVar(nameMap: Map<Variable, Variable>): Formula {
        if (allVariables.any { it in nameMap }) {
            return copyOf(children.map { it.renameAllVar(nameMap) })
        }
        return this
    }

    override fun renameAllVar(renamer: (Variable) -> Variable): Formula {
        return copyOf(children.map { it.renameAllVar(renamer) })
    }

    override fun renameVar(renamer: (Variable) -> Variable): Formula {
        return copyOf(children.map { it.renameVar(renamer) })
    }

    override fun regularizeVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        val newChildren = children.map { it.regularizeVarName(nameMap, nameProvider) }
        return copyOf(newChildren)
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

    override val bracketLevel: Int
        get() = 10


}

sealed class QualifiedFormula(child: Formula, val v: Variable) :
    UnaryFormula(child) {
    override val variables: Set<Variable> by lazy {
        child.variables - v
    }


    override fun isIdentityTo(other: Formula): Boolean {
        if (other !is QualifiedFormula) {
            return false
        }
        val c1 = child
        val c2 = other.child
        if (v == other.v) {
            return c1.isIdentityTo(c2)
        }

        val nv = Variable.getXNNameProvider("$").first {
            it !in c1.allVariables &&
                    it !in c2.allVariables
        }
        val f1 = c1.renameAllVar(mapOf(v to nv))
        val f2 = c2.renameAllVar(mapOf(other.v to nv))
        return f1.isIdentityTo(f2)
    }

    abstract fun copyOf(child: Formula, v: Variable): QualifiedFormula

    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        require(newChildren.size == 1)
        return copyOf(newChildren.first(), v)
    }

    override fun renameAllVar(nameMap: Map<Variable, Variable>): Formula {
        val nv = nameMap.getOrDefault(v, v)
        return copyOf(child.renameAllVar(nameMap), nv)
    }

    override fun renameAllVar(renamer: (Variable) -> Variable): Formula {
        val nv = renamer(v)
        return copyOf(child.renameAllVar(renamer),nv)
    }

    override fun renameVar(renamer: (Variable) -> Variable): Formula {
        val newRenamer : (Variable) -> Variable = {
            if(it == v){
                v
            }else{
                renamer(v)
            }
        }
        return copyOf(child.renameVar(newRenamer),v)
    }

    override fun regularizeVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        val nv = nameProvider.next()
        val original = nameMap[v]
        nameMap[v] = nv
        val newChild = child.regularizeVarName(nameMap, nameProvider)
        if (original != null) {
            nameMap[v] = original
        }
        return copyOf(newChild,nv)
    }

    override val bracketLevel: Int
        get() = 20
}

class NotFormula(child: Formula) : UnaryFormula(child) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return NotFormula(newChildren[0])
    }

    override val bracketLevel: Int
        get() = 5

    override fun toString(): String = "¬${wrapBracket(child)}"

    override fun regularizeVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        return NotFormula(child.regularizeVarName(nameMap, nameProvider))
    }
}

sealed class MultiFormula(children: List<Formula>) : CombinedFormula(children) {
    override fun flatten(): Formula {
        return if (childCount <= 1) {
            children.first().flatten()
        } else {
            val newChildren = ArrayList<Formula>(childCount)
            for (c in children) {
                val nc = c.flatten()
                if (nc is MultiFormula && nc::class == this::class) {
                    newChildren.addAll(nc.children)
                } else {
                    newChildren.add(nc)
                }
            }
            copyOf(newChildren)
        }
    }

    override val bracketLevel: Int
        get() = 15


}

class AndFormula(children: List<Formula>) : MultiFormula(children) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return AndFormula(newChildren)
    }

    override fun toString(): String {
        return children.joinToString(separator = "∧") { wrapBracket(it) }
    }
}

class OrFormula(children: List<Formula>) : MultiFormula(children) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return OrFormula(newChildren)
    }

    override fun toString(): String {
        return children.joinToString(separator = "∨") { wrapBracket(it) }
    }
}

class ForAnyFormula(child: Formula, v: Variable) : QualifiedFormula(child, v) {
    override fun copyOf(child: Formula, v: Variable): QualifiedFormula {
        return ForAnyFormula(child, v)
    }

    override fun toString(): String {
        return "∀${v.name} ${wrapBracket(child)}"
    }
}

class ExistFormula(child: Formula, v: Variable) : QualifiedFormula(child, v) {
    override fun copyOf(child: Formula, v: Variable): QualifiedFormula {
        return ExistFormula(child, v)
    }

    override fun toString(): String {
        return "∃${v.name} ${wrapBracket(child)}"
    }
}

class ImplyFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return ImplyFormula(newChildren[0], newChildren[1])
    }

    override fun toString(): String {
        return "${wrapBracket(child1)} → ${wrapBracket(child2)}"
    }
}

class EquivalentFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return EquivalentFormula(newChildren[0], newChildren[1])
    }

    override fun toString(): String {
        return "${wrapBracket(child1)} ↔ ${wrapBracket(child2)}"
    }
}

