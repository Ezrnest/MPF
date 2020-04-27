package cn.ancono.mpf.builder

import cn.ancono.mpf.core.*
import cn.ancono.mpf.core.Function
import cn.ancono.mpf.matcher.TMap


/*
 * Created by liyicheng at 2020-04-07 17:56
 */
abstract class TermBuilderContext {

//    val x =

    val String.c : Term
        get() = constance(QualifiedName(this))

    val String.v : Term
        get() = variable(this)

    val String.n : Term
        get() = named(QualifiedName(this))

    fun variable(name : String) : Term {
        return VarTerm(Variable(name))
    }

    fun constance(name : QualifiedName) : Term =
        ConstTerm(Constance(name))

    fun named(name : QualifiedName, vararg parameters : Variable) : Term =
        NamedTerm(name, parameters.toList())

    open operator fun String.invoke(vararg terms : Term) : Term {
        return FunTerm(
            Function(
                terms.size,
                QualifiedName(this)
            ), terms.asList()
        )
    }
}

object SimpleTermContext : TermBuilderContext(){
    @JvmField
    val a = "a".v

    @JvmField
    val b = "b".v

    @JvmField
    val c = "c".v

    @JvmField
    val A = "A".v

    @JvmField
    val B = "B".v

    @JvmField
    val C = "C".v

    @JvmField
    val x = "x".v

    @JvmField
    val y = "y".v

    @JvmField
    val X = "X".v

    @JvmField
    val Y = "Y".v
}
class RefTermContext(val context: TMap): TermBuilderContext(){

    @JvmField
    val x = "x".ref

    @JvmField
    val y = "y".ref

    @JvmField
    val X = "X".ref

    @JvmField
    val Y = "Y".ref

    val String.ref : Term
        get() = termRef(this)

    fun termRef(name : String) : Term {
        return context[name]?:throw NoSuchElementException("No term named `$name`")
    }
}

fun buildTerm(builderAction : TermBuilderContext.() -> Term) : Term = builderAction(
    SimpleTermContext
)