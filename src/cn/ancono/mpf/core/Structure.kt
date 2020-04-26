package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-04 19:27
 */
/**
 * Describes a predicate in a structure.
 * @author liyicheng
 */
class Predicate(val argLength: Int, val name: QualifiedName) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Predicate

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

/**
 * Describes a function in a structure.
 * @author liyicheng
 */
class Function(
    /**
     * The length of arguments of this function, -1 means the length of arguments is variable..
     */
    val argLength: Int,
    val name: QualifiedName
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Function

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}

data class Constance(val name: QualifiedName)

/**
 * Describes a structure of first order logic.
 */
interface Structure {

    val predicates: Set<Predicate>

    val functions: Set<Function>

    val constances: Set<Constance>

}

val EQUAL_PREDICATE = Predicate(2, QualifiedName("equals"))