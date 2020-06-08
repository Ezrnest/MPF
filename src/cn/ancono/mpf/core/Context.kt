package cn.ancono.mpf.core

import java.util.*


/*
 * Created by liyicheng at 2020-06-04 19:23
 */
class FormulaContext(
    val formulas: List<Formula>,
    val constants: Set<Constant>,
    val functions: Set<Function>,
    val predicates: Set<Predicate>,
    /**
     * Contains all the regular forms of the formulas in this context.
     */
    val regularForms: SortedSet<Formula>
) {


    fun addAll(fs : Collection<Formula>) : FormulaContext{
        val nfs = ArrayList<Formula>(formulas.size + fs.size)
        val nrfs = TreeSet(regularForms)
        nfs.addAll(formulas)
        for (f in fs) {
            val fr = f.toRegularForm()
            if (!regularForms.contains(fr)) {
                nfs += f
                nrfs += fr
            }
        }
        return FormulaContext(nfs,constants, functions, predicates, regularForms)
    }

    companion object {
        operator fun invoke(
            formulas: List<Formula>,
            constants: Set<Constant> = emptySet(),
            functions: Set<Function> = emptySet(),
            predicates: Set<Predicate> = emptySet()
        ): FormulaContext {
            val regularForms = sortedSetOf(FormulaComparator)
            for (f in formulas) {
                regularForms.add(f.toRegularForm())
            }
            return FormulaContext(formulas, constants, functions, predicates, regularForms)
        }
    }
}