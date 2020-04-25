package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-05 14:54
 */
data class QualifiedName(val displayName : String, val fullName : String){
    constructor(name : String) : this(name,name)
}
