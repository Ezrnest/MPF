package cn.ancono.mpf.core





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

    fun variable(name : String) : Term = VarTerm(Variable(name))

    fun constance(name : QualifiedName) : Term = ConstTerm(Contance(name))

    fun reference(name : QualifiedName) : Term = NamedTerm(name)

    operator fun String.invoke(vararg terms : Term) : Term {
        return FunTerm(Function(terms.size, QualifiedName(this)),terms.asList())
    }


    companion object{
        val EMPTY_CONTEXT : TermBuilderContext = TermBuilderContext()
    }
}



fun buildTerm(builderAction : TermBuilderContext.() -> Term) : Term = builderAction(TermBuilderContext.EMPTY_CONTEXT)