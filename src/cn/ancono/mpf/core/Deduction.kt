package cn.ancono.mpf.core

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
        return "${f}; by '${r.name.displayName}' with $dependencies"
    }
}


class DeductionNode(
    val deduction: Deduction,
    override val children: List<DeductionNode>
) : Node<DeductionNode> {

}