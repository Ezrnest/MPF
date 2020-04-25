package cn.ancono.mpf.core

import cn.ancono.mpf.matcher.MatchResult
import cn.ancono.mpf.matcher.Matcher


class TermMatchResult(
    val varMap: TMap
) : MatchResult {
}


interface TermMatcher : Matcher<Term,TermMatchResult> {
    fun match(term: Term, varMap: TMap): TermMatchResult?

    override fun match(x: Term, previousResult: TermMatchResult?): TermMatchResult? {
        return match(x,previousResult?.varMap ?: emptyMap())
    }

    val variables : Set<String>
}

class VarTermMatcher(val name: String) : TermMatcher {

    override val variables: Set<String> = setOf(name)


    override fun match(term: Term, varMap: TMap): TermMatchResult? {
        val required = varMap[name]
        return if (required == null) {
            TermMatchResult(varMap + (name to term))
        } else {
            if (term == required) {
                TermMatchResult(varMap)
            } else {
                null
            }
        }
    }
}

