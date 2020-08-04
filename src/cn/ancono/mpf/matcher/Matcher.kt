package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.Node

interface MatchResult
/*
 * Created by liyicheng at 2020-04-11 19:11
 */


/**
 * The general interface for matchers of tree structures.
 * Describes a matcher for a tree structure.
 * @author liyicheng
 */
interface Matcher<T : Node<T>, R : Any> {

    /**
     * Tries to match the given input node and returns a list of results.
     * If there is no result, returns an empty list.
     */
    fun match(x: T, previousResult: R?  = null): List<R>

    fun matchAll(x : T, previousResults : List<R>) : List<R>{
        return if (previousResults.isEmpty()) {
            match(x,null)
        }else{
            previousResults.flatMap { match(x,it) }
        }
    }
}


