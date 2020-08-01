package cn.ancono.mpf.core

import java.util.*


/**
 * The context of formulas, constants, functions and predicates.
 *
 * Created by liyicheng at 2020-06-04 19:23
 */
class FormulaContext(
    formulas: List<Formula>,
//    constants: Set<Constant>,
//    functions: Set<Function>,
//    predicates: Set<Predicate>,
    regularForms: NavigableMap<Formula, Formula>
) {
    private val fors: MutableList<Formula> = formulas.toMutableList()

    //    private val cons = constants.toMutableSet()
//    private val funs = functions.toMutableSet()
//    private val pres = predicates.toMutableSet()
    private val regs: NavigableMap<Formula, Formula> = TreeMap(regularForms)

    /**
     * The formulas reached.
     */
    val formulas: List<Formula>
        get() = fors
//    val constants: Set<Constant>
//        get() = cons
//    val functions: Set<Function>
//        get() = funs
//    val predicates: Set<Predicate>
//        get() = pres

    /**
     * Contains all the regular forms of the formulas in this context and their corresponding
     * original forms.
     */
    val regularForms: NavigableMap<Formula, Formula>
        get() = regs

    fun copy(): FormulaContext {
        return FormulaContext(formulas, regularForms)
    }


    fun addAll(fs: Collection<Formula>) {
        fors.addAll(fs)
        for (f in fs) {
            val fr = f.regularForm
            if (fr !in regs) {
                regs[fr] = f
            }
        }
    }

    fun addFormula(f: Formula) {
        fors.add(f)
        val fr = f.regularForm
        if (fr !in regs) {
            regs[fr] = f
        }
    }

    companion object {
        operator fun invoke(
            formulas: List<Formula>
//            constants: Set<Constant> = emptySet(),
//            functions: Set<Function> = emptySet(),
//            predicates: Set<Predicate> = emptySet()
        ): FormulaContext {
            val regularForms = TreeMap<Formula, Formula>(FormulaComparator)
            for (f in formulas) {
                val fr = f.regularForm
                regularForms[fr] = f
            }
            return FormulaContext(formulas, regularForms)
        }

        val EMPTY_CONTEXT = FormulaContext(emptyList())

    }
}

open class Context(
    val structure: MutableStructure,
    val formulaContext: FormulaContext = FormulaContext.EMPTY_CONTEXT,
    val reference: MutableMap<String, Formula> = mutableMapOf()
) {
}

class AssumedContext private constructor(
    val assumedFormulas: List<Formula>,
    structure: MutableStructure,
    formulaContext: FormulaContext = FormulaContext.EMPTY_CONTEXT,
    reference: MutableMap<String, Formula> = mutableMapOf()
) : Context(structure, formulaContext, reference) {
    companion object {
        fun from(assumedFormulas: List<Formula>, context: Context): AssumedContext {
            val fc = context.formulaContext.copy()
            fc.addAll(assumedFormulas)
            return AssumedContext(assumedFormulas, context.structure, fc, context.reference.toMutableMap())
        }
    }
}