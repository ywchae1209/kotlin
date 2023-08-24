package functions

import functions.FunctionUtils.curry

object FunctionUtils {

    fun <A,B,C> compose(f: (B) -> C, g: (A) -> B): (A) -> C = {
        a: A ->  f(g(a))
    }

    fun <A,B,C,D> curry(f: (A,B,C) -> D): (A) -> (B) -> (C) -> D = {
        a: A -> { b: B -> { c: C -> f(a, b, c)} }
    }

    fun <A,B,C> uncurry(f: (A) -> (B) -> C): (A, B) -> C = {
        a:A, b:B -> f(a)(b)
    }
}

fun main( args: Array<String>) {

    fun f( a: String, b: String, c: String) : String {
        return "($a, $b, $c)"
    }

    val z = curry(::f)("abc")("def")("hij")

    println(z)
}