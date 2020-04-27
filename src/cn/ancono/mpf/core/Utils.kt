package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-27 20:35
 */
object Utils {
    fun <T,R> collectionEquals(c1 : Collection<T>, c2 : Collection<R>, predicate : (T,R) -> Boolean) : Boolean{
        if(c1.size != c2.size){
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
}