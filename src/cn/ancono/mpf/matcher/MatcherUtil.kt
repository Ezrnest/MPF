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
    fun <T : Node<T>, R : Any> unorderedMatch(
        node: CombinedNode<T>, matchers: List<Matcher<T, R>>, fallback: Matcher<T, R>, previousResult: R?
    ): List<R> {
        if (node.childCount < matchers.size) {
            return emptyList()
        }
        val nodes = node.children
        val remains = BooleanArray(nodes.size) { true }
        val allResults = arrayListOf<R>()
        fun recurMatch(n: Int, currentResult: R?) {
            if (n >= matchers.size) {
                if (remains.any { it }) {
                    val remainPart = node.copyOf(nodes.filterIndexed { index, _ -> remains[index] })
                    allResults.addAll(fallback.match(remainPart, currentResult))
                } else {
                    if (currentResult != null) {
                        allResults += currentResult
                    }
                }
                return
            }
            val matcher = matchers[n]
            for (i in remains.indices) {
                if (!remains[i]) {
                    continue
                }
                val t = nodes[i]
                val results = matcher.match(t, currentResult)
                for (r in results) {
                    remains[i] = false
                    recurMatch(n + 1, r)
                    remains[i] = true
                }
            }
        }
        recurMatch(0, previousResult)
        return allResults
    }

    fun <T : Node<T>, R : Any> unorderedMatch(
        nodes: List<T>, matchers: List<Matcher<T, R>>, previousResult: R?
    ): List<R> {
        if (nodes.size != matchers.size) {
            return emptyList()
        }
        val remains = BooleanArray(nodes.size) { true }
        val allResults = arrayListOf<R>()
        fun recurMatch(n: Int, currentResult: R?) {
            if (n >= matchers.size) {
                if (currentResult != null) {
                    allResults.add(currentResult)
                }
                return
            }
            val matcher = matchers[n]
            for (i in remains.indices) {
                if (!remains[i]) {
                    continue
                }
                val t = nodes[i]
                val results = matcher.match(t, currentResult)
                for (r in results) {
                    remains[i] = false
                    recurMatch(n + 1, r)
                    remains[i] = true
                }
            }
        }
        recurMatch(0, previousResult)
        return allResults
    }

    fun <T : Node<T>, R : Any> orderedMatch(
        node: CombinedNode<T>,
        matchers: List<Matcher<T, R>>,
        previousResult: R?
    ): List<R> {
        return orderedMatch(node.children,matchers,previousResult)
    }

    fun <T : Node<T>, R : Any> orderedMatch(
        nodes: List<T>,
        matchers: List<Matcher<T, R>>,
        previousResult: R?
    ): List<R> {
        var results = if (previousResult != null) {
            listOf(previousResult)
        } else {
            emptyList()
        }
        for ((n, m) in nodes.zip(matchers)) {
            val r = m.matchAll(n,results)
            if (r.isEmpty()) {
                return emptyList()
            }
            results = r
        }
        return results
    }
}