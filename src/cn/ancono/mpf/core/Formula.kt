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
sealed class Formula {
    /**
     * Gets the free variables in this formula.
     */
    abstract val variables: Set<Variable>

    /**
     * Gets all the variables appearing in this formula.
     */
    abstract val allVariables : Set<Variable>

}

sealed class AtomicFormula : Formula()

/**
 * Describes a formula composed of a predicate, such as
 * ` a = b `, ` a < b `.
 */
class PredicateFormula(val p: Predicate, val terms: List<Term>) : AtomicFormula() {
    override val variables: Set<Variable> by lazy { terms.flatMapTo(hashSetOf()) { it.variables } }
    override val allVariables: Set<Variable>
        get() = variables
}
/**
 * Describes a formula with only a name and variables. This kind of formula is usually
 * an abbreviation of a complex formula. This formula is treated as atomic formula.
 */
class NamedFormula(val name: QualifiedName, override val variables: Set<Variable>) : AtomicFormula(){
    override val allVariables: Set<Variable>
        get() = variables
}

sealed class UnaryFormula(val child: Formula) : Formula() {
    override val variables: Set<Variable>
        get() = child.variables

    override val allVariables: Set<Variable>
        get() = child.allVariables
}


class NotFormula(child: Formula) : UnaryFormula(child)

sealed class CombinedFormula(val children: List<Formula>) : Formula() {
    override val variables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.variables } }

    override val allVariables: Set<Variable> by lazy { children.flatMapTo(hashSetOf()) { it.allVariables } }
}

class AndFormula(children: List<Formula>) : CombinedFormula(children)

class OrFormula(children: List<Formula>) : CombinedFormula(children)


sealed class BinaryFormula(val child1: Formula, val child2: Formula) : Formula() {
    override val variables: Set<Variable> by lazy { child1.variables union child2.variables }

    override val allVariables: Set<Variable> by lazy { child1.allVariables union child2.allVariables }

}

class ImplyFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2)

class EquivalentFormula(child1: Formula, child2: Formula) : BinaryFormula(child1, child2)

sealed class QualifiedFormula(child : Formula, val v : Variable) : UnaryFormula(child){
    override val variables: Set<Variable> by lazy {
        child.variables - v
    }

    override val allVariables: Set<Variable>
        get() = child.allVariables

}

class ExistFormula(child : Formula, v : Variable) : QualifiedFormula(child,v){}

class ForAnyFormula(child : Formula, v : Variable) : QualifiedFormula(child,v){}