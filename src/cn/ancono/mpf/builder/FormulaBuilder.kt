package cn.ancono.mpf.builder

import cn.ancono.mpf.core.*


fun buildFormula(builderAction : FormulaBuilder.() -> Formula) : Formula = builderAction(FormulaBuilder)

/*
 * Created by liyicheng at 2020-04-05 17:09
 */
object FormulaBuilder {

    operator fun String.unaryPlus() : NamedFormula = NamedFormula(QualifiedName(this), emptySet())

    infix fun Formula.and(g : Formula) : Formula = when(this){
        is AndFormula-> {
            when(g){
                is AndFormula -> AndFormula(this.children + g.children)
                else-> AndFormula(this.children + g)
            }
        }
        else -> {
            when(g){
                is AndFormula -> AndFormula( g.children + this)
                else-> AndFormula(listOf(this,g))
            }
        }
    }

    infix fun Formula.or(g : Formula) : Formula = when(this){
        is OrFormula-> {
            when(g){
                is OrFormula -> OrFormula(this.children + g.children)
                else-> OrFormula(this.children + g)
            }
        }
        else -> {
            when(g){
                is OrFormula -> OrFormula( g.children + this)
                else-> OrFormula(listOf(this,g))
            }
        }
    }

    operator fun Formula.not() = when(this){
        is NotFormula -> this.child
        else -> NotFormula(this)
    }

    infix fun Formula.implies(g : Formula) : Formula = ImplyFormula(this,g)

    infix fun Formula.equalTo(g : Formula) : Formula = EquivalentFormula(this,g)



//    fun exist(variable : String, )

}