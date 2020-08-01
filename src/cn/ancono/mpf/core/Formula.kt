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
         * Returns a name provider that returns names like "x1", "x2" ...
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

    /**
     * Recursively applies the given function as described in [recurMapMulti] with additional information.
     */
    abstract fun <T> recurMapMultiWith(f: (Formula) -> List<Pair<Formula, T>>): List<Pair<Formula, T>>

    abstract fun recurMap(f: (Formula) -> Formula): Formula

    /**
     * Recursively applies the given function to all the terms that appear in the formula.
     */
    abstract fun recurMapTerm(f: (Term) -> Term): Formula

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
    abstract fun renameAllVar(renamer: (Variable) -> Variable): Formula

    open fun renameAllVar(nameMap: Map<Variable, Variable>): Formula = renameAllVar { nameMap.getOrDefault(it, it) }



    internal abstract fun renameQualifiedVar0(
        nameMap: MutableMap<Variable, Variable>,
        nameProvider: Iterator<Variable>
    ): Formula

    /**
     * Renames only the free variables in this formula according to the map.
     */
    open fun renameVar(renamer: (Variable) -> Variable): Formula = replaceVar { VarTerm(renamer(it)) }

    open fun renameVar(nameMap: Map<Variable, Variable>): Formula = renameVar { nameMap.getOrDefault(it, it) }

    /**
     * Renames the qualified variables in this formula using the name provider in order.
     */
    fun regularizeQualifiedVar(nameProvider: Iterator<Variable>): Formula {
        return renameQualifiedVar0(hashMapOf(), nameProvider)
    }

    /**
     * Renames all the variables in this formula. The [nameProvider] should provide a sequence of non-duplicate names.
     * This method will returns a map of renamed free variables and their new names.
     *
     */
    fun regularizeAllVarName(nameProvider: Iterator<Variable> = Variable.getXNNameProvider().iterator()): Pair<Formula,Map<Variable,Variable>> {
        val map = mutableMapOf<Variable,Variable>()
        return regularizeAllVarName(map, nameProvider) to map
    }

    /**
     * Replace the free variables in this formula.
     */
    abstract fun replaceVar(replacer: (Variable) -> Term): Formula

    fun replaceVar(replaceMap: Map<Variable, Term>): Formula = replaceVar { replaceMap.getOrDefault(it, VarTerm(it)) }

    /**
     * Replaces all the named formulas in this formula with the given [mapper].
     */
    open fun replaceNamed(mapper: (NamedFormula) -> Formula): Formula {
        return recurMap { f ->
            if (f is NamedFormula) {
                mapper(f)
            } else {
                f
            }
        }
    }


    internal abstract fun regularizeAllVarName(
        nameMap: MutableMap<Variable, Variable>,
        nameProvider: Iterator<Variable>
    ): Formula

    /**
     * Converts this formula to the regular form, renaming constrained variables to `$1, $2 ...` and sorting sub-formulas.
     * This method requires that variables named like `$1` should not appear in the original formula.
     */
    val regularForm: Formula by lazy {
        if(variables.any { Helper.VarNamePattern.matchEntire(it.name) != null }){
            // conflicts
            val (f,m) = regularizeAllVarName(Variable.getXNNameProvider("_").iterator())
            val r = f.toRegularForm0(1).first
            val inv = hashMapOf<Variable,Variable>()
            for ((k, v) in m) {
                inv[v] = k
            }
            r.renameVar(inv)
        }else{
            toRegularForm0(1).first
        }


    }


    /**
     * Recursively converts the formula to regular, it is required that
     * the
     * @return a regular form and the next variable order
     */
    internal abstract fun toRegularForm0(varStart: Int): Pair<Formula, Int>

    /**
     * Returns a prenex normal form of the formula.
     *
     * See [PrenexNormalForm](https://mathworld.wolfram.com/PrenexNormalForm.html).
     *
     */
    abstract fun toPrenexForm(): Formula

    internal object Helper {
        val VarNamePattern = Regex("\\$(\\d+)")

        val FormulaPairComp: Comparator<Pair<Formula, Int>> = compareBy(FormulaComparator) {
            it.first
        }

        fun renameVarAfter(f: Formula, varStart: Int, newVarStart: Int): Formula {
            return f.renameAllVar { v ->
                val name = v.name
                val re = VarNamePattern.matchEntire(name)
                if (re != null) {
                    val (s) = re.destructured
                    val d = s.toInt()
                    val nd = if (d >= varStart) {
                        newVarStart + d - varStart
                    } else {
                        d
                    }
                    Variable("$$nd")
                } else {
                    v
                }
            }
        }
    }

    companion object {
        /**
         * Gets an unused variable with the provider.
         */
        fun nextVar(f: Formula, provider: Sequence<Variable> = Variable.getXNNameProvider()): Variable {
            val allNames = f.allVariables
            return provider.first {
                it !in allNames
            }
        }
    }


}

sealed class AtomicFormula : Formula(), AtomicNode<Formula> {
    override val childCount: Int
        get() = 0

    override fun recurMapMulti(f: (Formula) -> List<Formula>): List<Formula> {
        return f(this)
    }

    override fun <T> recurMapMultiWith(f: (Formula) -> List<Pair<Formula, T>>): List<Pair<Formula, T>> {
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


    override fun toPrenexForm(): Formula {
        return this
    }

    override fun renameQualifiedVar0(
        nameMap: MutableMap<Variable, Variable>,
        nameProvider: Iterator<Variable>
    ): Formula {
        return renameAllVar(nameMap)
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

        return Utils.collectionEquals(this.terms, other.terms, Term::isIdentityTo)
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


    override fun regularizeAllVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        val newTerms = terms.map { it.regularizeVarName(nameMap, nameProvider) }
        return PredicateFormula(p, newTerms)
    }

    override fun replaceVar(replacer: (Variable) -> Term): Formula {
        return PredicateFormula(p, terms.map { it.replaceVar(replacer) })
    }

    override fun toRegularForm0(varStart: Int): Pair<Formula, Int> {
        return PredicateFormula(p, terms.map { it.toRegularForm() }) to varStart
    }

    override fun recurMapTerm(f: (Term) -> Term): Formula {
        return PredicateFormula(p, terms.map(f))
    }
}

/**
 * Describes a formula with only a name and variables. This kind of formula is usually
 * an abbreviation of a complex formula. This formula is treated as atomic formula.
 */
class NamedFormula(val name: QualifiedName, val parameters: List<Term> = emptyList()) : AtomicFormula() {
    override val variables: Set<Variable> = parameters.flatMapTo(hashSetOf()) { it.variables }
    override val allVariables: Set<Variable>
        get() = variables

    override fun isIdentityTo(other: Formula): Boolean {
        if (other !is NamedFormula) {
            return false
        }
        return name == other.name && Utils.collectionEquals(parameters, other.parameters, Term::isIdentityTo)
    }

    override fun toString(): String {
        if (parameters.isEmpty()) {
            return name.displayName
        }
        return name.displayName + parameters.joinToString(",", prefix = "(", postfix = ")")
    }

    override fun renameAllVar(nameMap: Map<Variable, Variable>): Formula {
        if (allVariables.any { it in nameMap }) {
            return NamedFormula(name, parameters.map { it.renameVar(nameMap) })
        }
        return this
    }

    override fun renameVar(nameMap: Map<Variable, Variable>): Formula {
        return renameAllVar(nameMap)
    }

    override fun renameVar(renamer: (Variable) -> Variable): Formula {
        return renameAllVar(renamer)
    }

    override fun renameAllVar(renamer: (Variable) -> Variable): Formula {
        return NamedFormula(name, parameters.map { it.renameVar(renamer) })
    }

    override fun replaceVar(replacer: (Variable) -> Term): Formula {
        return NamedFormula(name, parameters.map { it.replaceVar(replacer) })
    }


    override fun regularizeAllVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
//        val nParameters = parameters.map<Variable, Variable> { v ->
//            var nv = nameMap[v]
//            if (nv == null) {
//                nv = nameProvider.next()
//                nameMap[v] = nv
//            }
//            nv
//        }
        val nParameters = parameters.map { it.regularizeVarName(nameMap, nameProvider) }
        return NamedFormula(name, nParameters)
    }

    override fun toRegularForm0(varStart: Int): Pair<Formula, Int> {
        return NamedFormula(name, parameters.map { it.toRegularForm() }) to varStart
    }

    override fun recurMapTerm(f: (Term) -> Term): Formula {
        return NamedFormula(name, parameters.map(f))
    }
}


sealed class CombinedFormula(override val children: List<Formula>, val ordered: Boolean = true) : Formula(),
    CombinedNode<Formula> {
    override val variables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.variables } }

    override val allVariables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.allVariables } }

    override fun isIdentityTo(other: Formula): Boolean {
        if (other !is CombinedFormula || this.javaClass != other.javaClass) {
            return false
        }
        return if (ordered) {
            Utils.collectionEquals(this.children, other.children, Formula::isIdentityTo)
        } else {
            Utils.listEqualsNoOrder(this.children, other.children, Formula::isIdentityTo)
        }

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

    override fun <T> recurMapMultiWith(f: (Formula) -> List<Pair<Formula, T>>): List<Pair<Formula, T>> {
        val possibleChildren = children.map { it.recurMapMultiWith(f) }
        val result = ArrayList<Pair<Formula, T>>(possibleChildren.sumBy { it.size })
        for (i in possibleChildren.indices) {
            for (c in possibleChildren[i]) {
                val newChildren = ArrayList(children)
                newChildren[i] = c.first
                result += copyOf(newChildren) to c.second
            }
        }
        result.addAll(f(this))
        return result
    }

    override fun recurMapTerm(f: (Term) -> Term): Formula {
        return copyOf(children.map { it.recurMapTerm(f) })
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

    override fun renameQualifiedVar0(
        nameMap: MutableMap<Variable, Variable>,
        nameProvider: Iterator<Variable>
    ): Formula {
        return copyOf(children.map { it.renameQualifiedVar0(nameMap, nameProvider) })
    }

    override fun replaceVar(replacer: (Variable) -> Term): Formula {
        return copyOf(children.map { it.replaceVar(replacer) })
    }

    override fun regularizeAllVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        val newChildren = children.map { it.regularizeAllVarName(nameMap, nameProvider) }
        return copyOf(newChildren)
    }

    override fun toPrenexForm(): Formula {
        TODO("Not yet implemented")
    }
}


sealed class UnaryFormula(child: Formula) : CombinedFormula(listOf(child)) {
    val child: Formula
        get() = children[0]

    override fun flatten(): Formula {
        return copyOf(children.map(Formula::flatten))
    }


}

sealed class BinaryFormula(child1: Formula, child2: Formula, ordered: Boolean) :
    CombinedFormula(listOf(child1, child2), ordered) {
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

    /**
     * Returns a copy of this qualified formula with exactly the given children and the variable.
     */
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
        return copyOf(child.renameAllVar(renamer), nv)
    }


    override fun renameVar(renamer: (Variable) -> Variable): Formula {
        val newRenamer: (Variable) -> Variable = {
            if (it == v) {
                v
            } else {
                renamer(it)
            }
        }
        return copyOf(child.renameVar(newRenamer), v)
    }

    override fun renameQualifiedVar0(
        nameMap: MutableMap<Variable, Variable>,
        nameProvider: Iterator<Variable>
    ): Formula {
        val nv = nameProvider.next()
        val original = nameMap[v]
        nameMap[v] = nv
        val newChild = child.renameQualifiedVar0(nameMap, nameProvider)
        if (original != null) {
            nameMap[v] = original
        }
        return copyOf(newChild, nv)
    }

    override fun replaceVar(replacer: (Variable) -> Term): Formula {
        val newReplacer: (Variable) -> Term = {
            if (it == v) {
                VarTerm(v)
            } else {
                replacer(it)
            }
        }
        return copyOf(child.replaceVar(newReplacer), v)
    }

    override fun regularizeAllVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        val nv = nameProvider.next()
        val original = nameMap[v]
        nameMap[v] = nv
        val newChild = child.regularizeAllVarName(nameMap, nameProvider)
        if (original != null) {
            nameMap[v] = original
        }
        return copyOf(newChild, nv)
    }

    override val bracketLevel: Int
        get() = 20

    override fun toRegularForm0(varStart: Int): Pair<Formula, Int> {
//        child.t
        val nv = Variable("$$varStart")
        val nameReplaced = child.renameVar {
            if (it == v) {
                nv
            } else {
                it
            }
        }

        val (r, n) = nameReplaced.toRegularForm0(varStart + 1)
        return copyOf(r, nv) to n
    }

    override fun recurMapTerm(f: (Term) -> Term): Formula {
        return copyOf(child.recurMapTerm(f), v)
    }
}

class NotFormula(child: Formula) : UnaryFormula(child) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return NotFormula(newChildren[0])
    }

    override val bracketLevel: Int
        get() = 5

    override fun toString(): String = "¬${wrapBracket(child)}"

    override fun regularizeAllVarName(nameMap: MutableMap<Variable, Variable>, nameProvider: Iterator<Variable>): Formula {
        return NotFormula(child.regularizeAllVarName(nameMap, nameProvider))
    }


    override fun toRegularForm0(varStart: Int): Pair<Formula, Int> {
        val (f, n) = child.toRegularForm0(varStart)
        return NotFormula(f) to n
    }

    override fun recurMapTerm(f: (Term) -> Term): Formula {
        return NotFormula(child.recurMapTerm(f))
    }

    override fun flatten(): Formula {
        val c = child.flatten()
        return if (c is NotFormula) {
            c.child
        }else{
            NotFormula(c)
        }
    }
}

sealed class MultiFormula(children: List<Formula>) : CombinedFormula(children, false) {
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


    override fun toRegularForm0(varStart: Int): Pair<Formula, Int> {
        val regulars = children.mapTo(sortedSetOf(Helper.FormulaPairComp)) { it.toRegularForm0(varStart) }
        var vs = varStart
        val newChildren = ArrayList<Formula>(regulars.size)
        for ((f, n) in regulars) {

            newChildren += Helper.renameVarAfter(f, varStart, vs)
            vs += n - varStart
        }
        return copyOf(newChildren) to vs
    }


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

class ImplyFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2, true) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return ImplyFormula(newChildren[0], newChildren[1])
    }

    override fun toString(): String {
        return "${wrapBracket(child1)} → ${wrapBracket(child2)}"
    }

    override fun toRegularForm0(varStart: Int): Pair<Formula, Int> {
        val (c1, n1) = child1.toRegularForm0(varStart)
        val (c2, n2) = child2.toRegularForm0(n1)
        return ImplyFormula(c1, c2) to n2
    }
}

class EquivalentFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2, false) {
    override fun copyOf(newChildren: List<Formula>): CombinedFormula {
        return EquivalentFormula(newChildren[0], newChildren[1])
    }

    override fun toString(): String {
        return "${wrapBracket(child1)} ↔ ${wrapBracket(child2)}"
    }

    override fun toRegularForm0(varStart: Int): Pair<Formula, Int> {
        val (f1, n1) = child1.toRegularForm0(varStart)
        val (f2, n2) = child2.toRegularForm0(varStart)
        val c1: Formula
        val c2: Formula
        if (FormulaComparator.compare(f1, f2) > 0) {
            c1 = f2
            c2 = Helper.renameVarAfter(f1, varStart, n2)
        } else {
            c1 = f1
            c2 = Helper.renameVarAfter(f2, varStart, n1)
        }
        return EquivalentFormula(c1, c2) to (n1 + n2 - varStart)
    }
}

object FormulaComparator : Comparator<Formula> {

    private fun ordinal(f: Formula): Int {
        return when (f) {
            is NamedFormula -> 0
            is PredicateFormula -> 1
            is NotFormula -> 2
            is ImplyFormula -> 3
            is EquivalentFormula -> 4
            is ForAnyFormula -> 5
            is ExistFormula -> 6
            is AndFormula -> 7
            is OrFormula -> 8
        }
    }

    private fun compareNamed(f1: NamedFormula, f2: NamedFormula): Int {
        return f1.name.compareTo(f2.name)
    }

    private fun comparePredicate(f1: PredicateFormula, f2: PredicateFormula): Int {
        val c = f1.p.name.compareTo(f2.p.name)
        if (c != 0) {
            return c
        }
        return Utils.compareCollectionLexi(f1.terms, f2.terms, Comparator.naturalOrder())
    }

    /**
     * Compare two binary formulas of the same type
     */
    private fun compareCombined(f1: CombinedFormula, f2: CombinedFormula): Int {
        return Utils.compareCollectionLexi(f1.children, f2.children, this)
    }

    override fun compare(f1: Formula, f2: Formula): Int {
        val c = ordinal(f1) - ordinal(f2)
        if (c != 0) {
            return c
        }
        return when (f1) {
            is NamedFormula -> compareNamed(f1, f2 as NamedFormula)
            is PredicateFormula -> comparePredicate(f1, f2 as PredicateFormula)
            is CombinedFormula -> compareCombined(f1, f2 as CombinedFormula)
        }
    }
}

/**
 * Collects all the constants appearing in this formula.
 */
fun Formula.allConstants(): List<Constant> {
    val constants = arrayListOf<Constant>()
    this.recurApply {
        if (it is PredicateFormula) {
            for (t in it.terms) {
                t.allConstantsTo(constants)
            }
        } else if (it is NamedFormula) {
            for (t in it.parameters) {
                t.allConstantsTo(constants)
            }
        }
        false
    }
    return constants
}