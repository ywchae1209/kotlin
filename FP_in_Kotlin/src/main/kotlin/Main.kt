import collections.LazyList
import collections.LazyList.Companion.drop
import collections.LazyList.Companion.take
import collections.LazyList.Companion.toSList
import collections.SList
import collections.SList.Companion.concat
import collections.SList.Companion.fill
import collections.SList.Companion.filter
import collections.SList.Companion.flatMap
import collections.SList.Companion.map
import collections.SList.Companion.take

fun main(args: Array<String>) {

    println("Program arguments: ${args.joinToString()}")

    ////////////////////////////////////////////////////////////////////////////////
    println("------------------------")
    val s = SList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
    println(s)

    println("------------------------")
    val s1 = s.take(3)//.map{ "*$it*" }
    println(s1)

    println("------------------------")
    val s2 = s.take(2)
    println(s2)

    println("------------------------")
    val s3 = s2.flatMap{ _ -> s}
    println(s3)

    println("------------------------")


    ////////////////////////////////////////////////////////////////////////////////
    val a = LazyList.of(1,2,3,4,5,6,7)

    println(a.take(3))
    println(a.drop(3).toSList())

}