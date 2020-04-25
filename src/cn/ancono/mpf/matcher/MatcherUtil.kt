package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.CombinedNode
import cn.ancono.mpf.core.Node


/*
 * Created by liyicheng at 2020-04-11 20:51
 */
/**
 * Provides some utilities for matcher.
 */
object MatcherUtil {
    fun <T : Node<T>, R : MatchResult> unorderedMatch(
        node: CombinedNode<T>, matchers: List<Matcher<T, R>>, fallback: Matcher<T, R>, previousResult : R?
    ): R? {
        if (node.childCount < matchers.size) {
            return null
        }
        val nodes = node.children
        val remains = BooleanArray(nodes.size) { true }
        fun recurMatch(n: Int, currentResult: R?): R? {
            if (n >= matchers.size) {
                if (remains.any { it }) {
                    val remainPart = node.copyOf(nodes.filterIndexed { index, _ -> remains[index] })
                    return fallback.match(remainPart, currentResult)
                }
                return currentResult
            }
            val matcher = matchers[n]
            for (i in remains.indices) {
                if (!remains[i]) {
                    continue
                }
                val t = nodes[i]
                val result = matcher.match(t, currentResult) ?: continue
                remains[i] = false
                val result2 = recurMatch(n + 1, result)
                remains[i] = true
                if (result2 != null) {
                    return result2
                }
            }
            return null
        }
        return recurMatch(0, previousResult)
    }

    fun <T : Node<T>, R : MatchResult> unorderedMatch(
        nodes: List<T>, matchers: List<Matcher<T, R>>, previousResult : R?
    ): R? {
        if (nodes.size != matchers.size) {
            return null
        }
        val remains = BooleanArray(nodes.size) { true }
        fun recurMatch(n: Int, currentResult: R?): R? {
            if (n >= matchers.size) {
                return currentResult
            }
            val matcher = matchers[n]
            for (i in remains.indices) {
                if (!remains[i]) {
                    continue
                }
                val t = nodes[i]
                val result = matcher.match(t, currentResult) ?: continue
                remains[i] = false
                val result2 = recurMatch(n + 1, result)
                remains[i] = true
                if (result2 != null) {
                    return result2
                }
            }
            return null
        }
        return recurMatch(0, previousResult)
    }

    fun <T : Node<T>, R : MatchResult> orderedMatch(
        node: CombinedNode<T>,
        matchers: List<Matcher<T, R>>,
        previousResult : R?
    ): R? {
        return node.children.zip(matchers).fold(previousResult) { re, (n, m) ->
            m.match(n, re)
        }
    }

    fun <T : Node<T>, R : MatchResult> orderedMatch(
        nodes: List<T>,
        matchers: List<Matcher<T, R>>,
        previousResult : R?
    ): R? {
        return nodes.zip(matchers).fold(previousResult) { re, (n, m) ->
            m.match(n, re)
        }
    }
}