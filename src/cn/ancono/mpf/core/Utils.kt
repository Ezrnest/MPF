package cn.ancono.mpf.core


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
}
//
//fun main() {
//    val l1 = listOf(1,2,3)
//    val l2 = listOf(3,1,2)
//    println(Utils.listEqualsNoOrder(l1,l2,Any::equals))
//}