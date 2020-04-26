package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.*
import cn.ancono.mpf.core.Function


class TermMatchResult(
    val varMap: TMap
) : MatchResult {
}

fun TermMatchResult?.toNonNull() : TermMatchResult{
    return this?: TermMatchResult(emptyMap())
}


interface TermMatcher : Matcher<Term, TermMatchResult> {
    val variables: Set<String>
}

class RefTermMatcher(val refName: String) : TermMatcher {

    override val variables: Set<String> = setOf(refName)
    override fun match(x: Term, previousResult: TermMatchResult?): TermMatchResult? {
        val varMap = previousResult?.varMap ?: emptyMap()
        val required = varMap[refName]
        return if (required == null) {
            TermMatchResult(varMap + (refName to x))
        } else {
            if (x == required) {
                TermMatchResult(varMap)
            } else {
                null
            }
        }
    }
}

class VarTermMatcher(val variable: Variable) : TermMatcher{
    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): TermMatchResult? {
        return if(x is VarTerm && x.v == variable){
            previousResult.toNonNull()
        }else{
            null
        }
    }
}


class ConstTermMatcher(val c: Constance) : TermMatcher {

    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): TermMatchResult? {
        return if (x is ConstTerm && x.c == c) {
            previousResult.toNonNull()
        } else {
            null
        }
    }

}

class FunTermMatcher(val function: Function, val subMatchers: List<TermMatcher>,val ordered : Boolean) : TermMatcher {
    override fun match(x: Term, previousResult: TermMatchResult?): TermMatchResult? {
        if (x !is FunTerm || x.f != function) {
            return null
        }
        return if (ordered) {
            MatcherUtil.orderedMatch(x.children,subMatchers,previousResult)
        }else{
            MatcherUtil.unorderedMatch(x.children,subMatchers,previousResult)
        }
    }

    override val variables: Set<String> by lazy { subMatchers.flatMapTo(hashSetOf()) { it.variables } }
}

