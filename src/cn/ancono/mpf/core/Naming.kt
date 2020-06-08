package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-05 14:54
 */
data class QualifiedName(val displayName : String, val fullName : String) : Comparable<QualifiedName>{
    constructor(name : String) : this(name,name)

    override fun compareTo(other: QualifiedName): Int {
        return fullName.compareTo(other.fullName)
    }
}
