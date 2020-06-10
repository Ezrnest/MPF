package cn.ancono.mpf.kt

import cn.ancono.mpf.core.Structure
import cn.ancono.mpf.core.System


/*
 * Created by liyicheng at 2020-06-10 20:17
 */
object MpfKt {

    fun mpf(structure: Structure, action : DeductionScope.() -> Unit) {
        val system = System(structure)
        val deduction = DeductionScope(system)
        action(deduction)
    }
}