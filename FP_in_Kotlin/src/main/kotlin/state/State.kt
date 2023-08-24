package state

import collections.SList
import collections.SList.Companion.cons
import collections.SList.Companion.empty
import collections.SList.Companion.foldRight
import collections.SList.Companion.map
import state.State.Companion.sequence

import state.CoinMachine.simulateMachine
import state.Random.gen_Ints
import state.State.Companion.get
import state.State.Companion.map2
import kotlin.math.abs

data class State<S, out A>(val run: (S) -> Pair<A, S>) {

    fun <B> flatMap(f: (A) -> State<S,B>) : State<S,B> = State { s ->
        val (a, s) = run(s)
        f(a).run(s)
    }

    fun <B> map(f: (A) -> B): State<S,B> = State { s ->
        val (a, s) = run(s)
        f(a) to s
    }

    // theoretically right.. but, need boxing/unboxing
    fun <B> map_(f: (A) -> B): State<S,B> =
        flatMap{ a -> unit(f(a)) }

    companion object {
        fun <S,A> unit(a: A): State<S,A> = State { s -> a to s }

        fun <S,A,B,C> map2( sa: State<S,A>, sb: State<S,B>, f: (A,B) -> C) : State<S,C> =
            sa.flatMap{ a ->
                sb.map{ b ->
                    f(a, b)
                }
            }

        fun <S,A> SList<State<S, A>>.sequence(): State<S, SList<A>> =
            this.foldRight( unit( empty())){ a, b -> map2(a, b){ c, d ->
                cons(c, d)}
            }

        // think about map2 of Applicative Functor...
        fun <S,A,B,C> map2Ap( sa: State<S,A>, sb: State<S,B>, f: (A,B) -> C) : State<S,C> = State { s ->
            val (a, s1) = sa.run(s)
            val (b, s2) = sb.run(s)
            f(a,b) to s1        // which one to choose ?
        }
        fun <S> get(): State<S,S> = State { s -> s to s }
        fun <S> set(s: S): State<S,Unit> = State { _ -> Unit to s }
        fun <S> modify(f: (S) -> S): State<S, Unit> = get<S>().flatMap { s -> set(s) }
    }
}

object Random {

    interface RNG {
        fun nextInt() : Pair<Int, RNG>
    }

    data class SimpleRNG(val seed: Long) : RNG {
        override fun nextInt(): Pair<Int, RNG> {
            val newSeed = (seed * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
            val nextRNG = SimpleRNG(newSeed)
            val n = (newSeed ushr 16).toInt()
            return n to nextRNG
        }
    }

    private fun int() : State<RNG, Int> = State{ it.nextInt() }
    private fun double()  = int().map{ it / Int.MAX_VALUE.toDouble() + 1}
    private fun ints(n: Int) = SList.fill(n, Unit).map{ _ -> int()}.sequence()
    private fun int_double() = map2( int(), double()){ a, b -> a to b}
    private fun int_Abs() = int().map{ abs( it) }

    fun gen_Ints(n: Int, seed: Long = 12345L) =
        ints(n).run(SimpleRNG(seed)).first

}

object CoinMachine {

    sealed interface Input
    object Coin: Input {
        override fun toString(): String = "Coin"
    }
    object Turn: Input {
        override fun toString(): String = "Turn"
    }

    data class Machine( val locked: Boolean, val candies: Int, val coins: Int)

    fun <A> show( a: A, msg: String, show: Boolean = true) =
         if(show) {
            println(msg)
             a
         } else a

    fun update( input: Input) : State<Machine, Input> = State { m ->
        input to when(input) {
            is Coin ->
                if( !m.locked || m.candies <= 0) show(m, "$input : not-locked, get a candy first")
                else show( Machine( false, m.candies, m.coins + 1 ), "$input : coin+ : ${m.coins + 1}")

            is Turn ->
                if( m.locked || m.candies <= 0) show(m, "$input : locked, insert a coin first")
                else show( Machine( true, m.candies - 1, m.coins), "$input : candis- : ${m.candies-1}" )
        }
    }

    fun simulateMachine( inputs: SList<Input>) =
        inputs
            .map{ update(it)}
            .sequence()
            .flatMap{ _ -> get<Machine>()}
            .map{ it.coins to it.candies}

}

/// CoinMachine example
fun main(args: Array<String>) {

    val size = 100
    val coins = 30
    val candies = 30

    val ints = gen_Ints(size)

    val inputs = ints.map{ if(0 == it % 2) CoinMachine.Coin else CoinMachine.Turn }
    println( "input scenario : size = $size : $inputs")

    val init = CoinMachine.Machine( true, candies, coins)
    println( "Candy Machine Initial State : $init")

    val result = simulateMachine(inputs).run(init)

    println("::: Last result after simulation :::" +
            "(machine state) = ${result.second}")
}
