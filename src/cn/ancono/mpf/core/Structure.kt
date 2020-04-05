package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-04 19:27
 */
/**
 * Describes a predicate in a structure.
 * @author liyicheng
 */
data class Predicate(val argLength : Int, val name : QualifiedName) {

}
/**
 * Describes a function in a structure.
 * @author liyicheng
 */
data class Function(val argLength : Int, val name : QualifiedName){
}

data class Contance(val name: QualifiedName)

/**
 * Describes a structure of first order logic.
 */
interface Structure {

    val predicates : Set<Predicate>

    val functions : Set<Function>

    val contances : Set<Contance>

}