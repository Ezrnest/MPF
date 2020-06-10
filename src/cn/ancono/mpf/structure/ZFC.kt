package cn.ancono.mpf.structure

import cn.ancono.mpf.core.Predicate
import cn.ancono.mpf.core.QualifiedName


/*
 * Created by liyicheng at 2020-04-26 15:49
 */
val IN_PREDICATE = Predicate(2, QualifiedName.of("in",ZFC.namespace),true)

object ZFC {
    val namespace = "zfc"
}