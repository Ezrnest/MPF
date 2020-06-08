package cn.ancono.mpf.core

import java.util.*


/*
 * Created by liyicheng at 2020-04-27 20:35
 */
object Utils {
    fun <T, R> collectionEquals(c1: Collection<T>, c2: Collection<R>, predicate: (T, R) -> Boolean): Boolean {
        if (c1.size != c2.size) {
            return false
        }
        val it1 = c1.iterator()
        val it2 = c2.iterator()
        while (it1.hasNext()) {
            if (!predicate(it1.next(), it2.next())) {
                return false
            }
        }
        return true
    }

    fun <T, R> listEqualsNoOrder(list1: List<T>, list2: List<R>, predicate: (T, R) -> Boolean): Boolean {
        if (list1.size != list2.size) {
            return false
        }
        val size = list1.size
        val matched = BooleanArray(size) { false }
        fun recurMatch(i : Int) : Boolean{
            if (i >= size) {
                return true
            }
            val a = list1[i]
            for ((j, b) in list2.withIndex()) {
                if(matched[j]){
                    continue
                }
                if(predicate(a,b)){
                    matched[j] = true
                    if(recurMatch(i+1)){
                        return true
                    }
                    matched[j] = false
                }
            }
            return false
        }
        return recurMatch(0)
    }

    /**
     * Compares two collections by their lexicographical order using a comparator.
     *
     *
     * For example, [-5,1] < [-1,2,3] and [1,2,3] < [1,2,3,4]
     */
    fun <T> compareCollectionLexi(
        list1: Collection<T>,
        list2: Collection<T>,
        comp: Comparator<T>
    ): Int {
        val it1 = list1.iterator()
        val it2 = list2.iterator()
        while (it1.hasNext() && it2.hasNext()) {
            val a = it1.next()
            val b = it2.next()
            val com = comp.compare(a, b)
            if (com != 0) {
                return com
            }
        }
        if (it1.hasNext()) {
            return 1
        }
        return if (it2.hasNext()) {
            -1
        } else 0
    }
}
//
//fun main() {
//    val l1 = listOf(1,2,3)
//    val l2 = listOf(3,1,2)
//    println(Utils.listEqualsNoOrder(l1,l2,Any::equals))
//}