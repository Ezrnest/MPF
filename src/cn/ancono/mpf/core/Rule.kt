package cn.ancono.mpf.core

import cn.ancono.mpf.matcher.FormulaMatcher

/*
 * Created by liyicheng at 2020-04-05 19:10
 */




/**
 * Describes the transformation rule in a structure.
 * @author liyicheng
 */
interface Rule{
//    val arguments : List<ArgumentType>

    /**
     * Applies this rule to the given formulas and terms, tries to return the desired result.
     */
    fun apply(formulas : List<Formula>, terms : List<Term>, desiredResult : Formula?) : Formula?

    val inputMatcher : FormulaMatcher


}