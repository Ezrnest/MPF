package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.Node

interface MatchResult
/*
 * Created by liyicheng at 2020-04-11 19:11
 */


/**
 * Describes a matcher for a tree structure.
 * @author liyicheng
 */
interface Matcher<T : Node<T>, R : MatchResult> {

    /**
     * Tries to match the given input node.
     */
    fun match(x: T, previousResult: R? = null): R?
}


