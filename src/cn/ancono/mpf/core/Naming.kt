package cn.ancono.mpf.core


/*
 * Created by liyicheng at 2020-04-05 14:54
 */
class QualifiedName(val displayName : String, val fullName : String) : Comparable<QualifiedName>{
    constructor(name : String) : this(name,name)

    override fun compareTo(other: QualifiedName): Int {
        return fullName.compareTo(other.fullName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QualifiedName

        if (fullName != other.fullName) return false

        return true
    }

    override fun hashCode(): Int {
        return fullName.hashCode()
    }

    override fun toString(): String {
        return "Name($fullName)"
    }


    companion object{
        fun of(name: String, vararg qualifiers : String) : QualifiedName{
            val fullname = buildString {
                for (q in qualifiers) {
                    append(q).append('.')
                }
                append(name)
            }
            return QualifiedName(name,fullname)
        }

        fun parseQualified(fullName: String) : QualifiedName{
            val idx = fullName.lastIndexOf('.')
            val name = fullName.substring(idx+1)
            require(name.isNotBlank())
            return QualifiedName(name,fullName)
        }
    }
}
