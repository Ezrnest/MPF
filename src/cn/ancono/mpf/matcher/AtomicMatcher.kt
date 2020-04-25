package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.CombinedNode
import cn.ancono.mpf.core.Node


/*
 * Created by liyicheng at 2020-04-11 19:50
 */
/**
 * @author liyicheng
 */
interface AtomicMatcher<T : Node<T>, R : MatchResult> : Matcher<T,R> {
}

interface CombinedMatcher<T : Node<T>, R : MatchResult> : Matcher<T,R>{}

open class UnorderedMatcher<T : Node<T>, R : MatchResult>(
    val children: List<Matcher<T, R>>,
    val fallback: Matcher<T, R>
) : CombinedMatcher<T, R> {
    override fun match(x: T, previousResult: R?): R? {
        if (x !is CombinedNode<*>) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        val cb = x as CombinedNode<T>
        return MatcherUtil.unorderedMatch(cb, children, fallback, previousResult)
    }
}


open class OrderedMatcher<T : Node<T>, R : MatchResult>(val children: List<Matcher<T, R>>) : CombinedMatcher<T, R> {
    override fun match(x: T, previousResult: R?): R? {
        if (x !is CombinedNode<*>) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        val cb = x as CombinedNode<T>
        return MatcherUtil.orderedMatch(cb, children, previousResult)
    }
}

//open class OrderedMatcher<T: Node<T>, R : MatchResult>(val children : List<Matcher<T,R>>)
//    : Matcher<T,R>{
//    override fun match(x: T, previousResult: R?): R? {
//        if (x !is CombinedNode<*>) {
//            return null
//        }
//        return MatcherUtil.orderedMatch(x,children,previousResult)
//    }
//}
