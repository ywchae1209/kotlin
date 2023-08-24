import collections.LazyList
import collections.LazyList.Companion.drop
import collections.LazyList.Companion.forAll
import collections.LazyList.Companion.infinite
import collections.LazyList.Companion.take
import collections.LazyList.Companion.toSList
import collections.SList
import collections.SList.Companion.flatMap
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

    val f = {a: String, b: Int -> a.toInt() to b }
    println(f.show)


    val inf = infinite( 5 ).take(1)
    inf.forAll{
        println(it)
        it < 7
    }

    val g1 = OperatorExample.from("1111")
    val g2 = OperatorExample.from("2222")

    g1("b")
    val z2 =g1 - g2
    val z1 =g1 + g2
}

val <A,B,C> ((A,C) -> B).show : String
    get() = this.toString()

interface OperatorExample {
    operator fun invoke(s: String) : String
    operator fun minus(o: OperatorExample): OperatorExample
    operator fun plus(o: OperatorExample): OperatorExample

    companion object {
        fun from(st: String): OperatorExample =
            object : OperatorExample {
                override fun invoke(s: String): String {
                    val ret = "called: $st\t$s"
                    println(ret)
                    return ret
                }

                override fun minus(o: OperatorExample): OperatorExample {
                    println("minus")
                    return o
                }

                override fun plus(o: OperatorExample): OperatorExample {
                    println("plus")
                    return this
                }
            }
    }
}