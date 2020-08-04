package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-11 19:17
 */
/**
 * Describes a node in a tree structure.
 * @author liyicheng
 */
interface Node<T : Node<T>> {
    val childCount: Int
        get() = children.size
    val children: List<T>



    /**
     * Applies the function recursively to each node in this node. The order of
     * iteration is pre-order. The function may return `true` to indicate that the iterating process
     * should be stopped.
     *
     * @param f a function to apply to the sub-nodes and may return `true` to indicate the process should be stopped
     *
     * @return `true` if the process should be stopped, `false` to continue.
     */
    fun recurApply(f: (T) -> Boolean): Boolean {
        @Suppress("UNCHECKED_CAST")
        if (f(this as T)) {
            return true
        }
        return children.any {
            it.recurApply(f)
        }
    }

    /**
     * Recursively maps the tree of nodes to a new one. The children will be recursively mapped first and the
     * resulting children will be used to construct a new node, which will be passed to [f].
     *
     * The following code shows the logic of this function:
     *
     *     fun recurMap(f : (T) -> T) :T{
     *         val mappedChildren = children.map { c -> c.recurMap(f)}
     *         val mapped = this.copyOf(mappedChildren)
     *         return f(mapped)
     *     }
     *
     * @return a recursively mapped new node as the result
     * @param f the mapping function, it should only focus on the given node.
     */
    fun recurMap(f : (T) -> T) :T


}

interface AtomicNode<T : Node<T>> : Node<T> {
    override val childCount: Int
        get() = 0
    override val children: List<T>
        get() = emptyList()

    override fun recurApply(f: (T) -> Boolean): Boolean {
        @Suppress("UNCHECKED_CAST")
        return f(this as T)
    }
}

interface CombinedNode<T : Node<T>> : Node<T> {
    fun copyOf(newChildren: List<T>): T
}


