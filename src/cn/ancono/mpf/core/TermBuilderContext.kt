package cn.ancono.mpf.core

import cn.ancono.mpf.matcher.TMap


/*
 * Created by liyicheng at 2020-04-07 17:56
 */
open class TermBuilderContext(val context: TMap = emptyMap()) {

//    val x =

    val String.c : Term
        get() = constance(QualifiedName(this))

    val String.v : Term
        get() = variable(this)

    val String.ref : Term
        get() = reference(QualifiedName(this))

    fun variable(name : String) : Term {
        return context[name] ?: VarTerm(Variable(name))
    }

    fun constance(name : QualifiedName) : Term = ConstTerm(Constance(name))

    fun reference(name : QualifiedName, vararg parameters : Variable) : Term = NamedTerm(name,parameters.toList())

    operator fun String.invoke(vararg terms : Term) : Term {
        return FunTerm(Function(terms.size, QualifiedName(this)),terms.asList())
    }


    companion object{
        val EMPTY_CONTEXT : TermBuilderContext = TermBuilderContext()
    }
}



fun buildTerm(builderAction : TermBuilderContext.() -> Term) : Term = builderAction(TermBuilderContext.EMPTY_CONTEXT)