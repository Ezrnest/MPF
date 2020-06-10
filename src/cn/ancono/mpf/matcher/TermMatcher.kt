package cn.ancono.mpf.matcher

import cn.ancono.mpf.core.*
import cn.ancono.mpf.core.Function
import java.lang.UnsupportedOperationException


class TermMatchResult(
    val varMap: TMap
) : MatchResult {
}


fun TermMatchResult?.toNonNull(): TermMatchResult {
    return this ?: TermMatchResult(emptyMap())
}


interface TermMatcher : Matcher<Term, TermMatchResult> {
    val variables: Set<String>

    companion object {
        fun fromTerm(t: Term): TermMatcher {
            return when (t) {
                is ConstTerm -> {
                    FixedConstTermMatcher(t.c)
                }
                is VarTerm -> {
                    RefTermMatcher(t.v.name)
                }
                is FunTerm -> {
                    val f = t.f
                    FunTermMatcher(f, t.children.map { fromTerm(it) }, f.ordered)
                }
                else -> throw UnsupportedOperationException("The term type '${t.javaClass.name}' is not supported.")
            }
        }
    }
}

object EmptyTermMatcher : TermMatcher {
    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        return emptyList()
    }
}

object WildcardTermMatcher : TermMatcher {
    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        return listOf(previousResult.toNonNull())
    }
}


class RefTermMatcher(val refName: String) : TermMatcher {

    override val variables: Set<String> = setOf(refName)


    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        val varMap = previousResult?.varMap ?: emptyMap()
        val required = varMap[refName]
        return if (required == null) {
            listOf(TermMatchResult(varMap + (refName to RefTerm(x))))
        } else {
            if (x.isIdentityTo(required.term)) {
                listOf(TermMatchResult(varMap))
            } else {
                emptyList()
            }
        }
    }
}

class FixedVarTermMatcher(val variable: Variable) : TermMatcher {
    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        return if (x is VarTerm && x.v == variable) {
            listOf(previousResult.toNonNull())
        } else {
            emptyList()
        }
    }
}


class FixedConstTermMatcher(val c: Constant) : TermMatcher {

    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        return if (x is ConstTerm && x.c == c) {
            listOf(previousResult.toNonNull())
        } else {
            emptyList()
        }
    }
}

object AnyVarTermMatcher : TermMatcher {
    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        return if (x is VarTerm) {
            listOf(previousResult.toNonNull())
        } else {
            emptyList()
        }
    }
}

object AnyConstTermMatcher : TermMatcher {
    override val variables: Set<String>
        get() = emptySet()

    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        return if (x is ConstTerm) {
            return listOf(previousResult.toNonNull())
        } else {
            emptyList()
        }
    }
}

class FunTermMatcher(val function: Function, val subMatchers: List<TermMatcher>, val ordered: Boolean) : TermMatcher {
    override fun match(x: Term, previousResult: TermMatchResult?): List<TermMatchResult> {
        if (x !is FunTerm || x.f != function) {
            return emptyList()
        }
        return if (ordered) {
            MatcherUtil.orderedMatch(x.children, subMatchers, previousResult)
        } else {
            MatcherUtil.unorderedMatch(x.children, subMatchers, previousResult)
        }
    }

    override val variables: Set<String> by lazy { subMatchers.flatMapTo(hashSetOf()) { it.variables } }
}

//class NamedTermMatcher(val name : QualifiedName)
// TODO
