import collections.SList
import collections.SList.Companion.fill
import collections.SList.Companion.filter
import collections.SList.Companion.flatMap
import collections.SList.Companion.map
import collections.SList.Companion.take

fun main(args: Array<String>) {

    println("Program arguments: ${args.joinToString()}")

    ////////////////////////////////////////////////////////////////////////////////
    val s = SList.of(1, 2, 3, 4, 5).map { it + 10 }.filter { it % 2 == 0 }.map { it * 2 }
    println(s)

    println("------------------------")

    val s1 = s.take(3).map{ "*$it*" }
    println(s1)

    println("------------------------")

    val s2 = s.flatMap{ fill( it, "$it" )}
    println(s2)

    println("------------------------")
}