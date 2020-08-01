package cn.ancono.mpf.core

import java.lang.StringBuilder

/**
 * A deduction result recording the formula reached and context used.
 */
class Deduction(
    val r: Rule,
    /**
     * The resulting formula.
     */
    val f: Formula,
    /**
     * The formulas from which the result is reached.
     */
    val dependencies: List<Formula>,
    /**
     * Additional information about the result, default to an empty map.
     */
    val moreInfo: Map<String, Any> = emptyMap()

) {

    override fun toString(): String {
        return "${f}  | by '${r.name.displayName}' with $dependencies"
    }
}


class DeductionNode(
    val deduction: Deduction,
    override val children: List<DeductionNode>
) : Node<DeductionNode> {

    fun appendTo(sb: StringBuilder, level: Int, indent: Int) {
        repeat(level * indent) {
            sb.append(' ')
        }


        sb.append(deduction.f).append("; by '")
            .append(deduction.r.name.displayName)
            .append("'")
        if (childCount == 0) {
            sb.append(" with ")
            deduction.dependencies.joinTo(sb)
        }
        sb.appendln()
        for (n in children) {
            n.appendTo(sb, level + 1, indent)
        }


    }

    fun toStringAsTree(indent: Int = 2): String {
        val sb = StringBuilder()
        appendTo(sb, 0, indent)
        return sb.toString()
    }

}