package cn.ancono.mpf.core


class RuleHints(val ruleName : QualifiedName){

}

class Deduction(val r : Rule,
                val result: Result
){
    val f : Formula
        get() = result.f
    val moreInfo : Map<String,Any>
        get() = result.moreInfo
    val dependencies : List<Formula>
        get() = result.dependencies

    override fun toString(): String {
        return "${result.f}; by '${r.name.displayName}' with ${result.dependencies}"
    }
}
/**
 * The system is the core of the deduction system.
 *
 * Created by liyicheng at 2020-06-09 15:43
 */
class System(val structure : Structure){
    val contextStack = arrayListOf<Context>()

    init{
        contextStack.add(Context())
    }

    val context : Context
        get() = contextStack.last()


    /**
     * Create
     */
    fun assumeContext(fs : List<Formula>){
        //TODO
    }

    fun addFormula(f: Formula) {
        context.formulaContext.addFormula(f)
    }

    /**
     * Tries to deduce the required formula [f] using the given rule hints.
     */
    fun deduce(f : Formula, hints : RuleHints) : Deduction?{
        val candidate = structure.ruleMap[hints.ruleName]
        val rules = if(candidate != null){
            listOf(candidate)
        }else{
            structure.defaultRules.values
        }
        for (rule in rules) {
            val tr = rule.applyToward(context.formulaContext, emptyList(), emptyList(),f)
            if (tr is Reached) {
                addFormula(f)
                return Deduction(rule,tr.result)
            }
        }

        return null
    }

}