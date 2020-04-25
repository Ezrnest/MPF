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
    val children: List<T>
}

interface AtomicNode<T : Node<T>> : Node<T> {
    override val childCount: Int
        get() = 0
    override val children: List<T>
        get() = emptyList()
}

interface CombinedNode<T : Node<T>> : Node<T> {
    fun copyOf(newChildren: List<T>): T
}


