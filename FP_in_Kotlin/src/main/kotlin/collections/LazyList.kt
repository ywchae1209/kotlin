package collections

import collections.Maybe.Companion.getOrElse
import collections.Maybe.Companion.just
import collections.Maybe.Companion.map
import collections.Maybe.Companion.none
import collections.SList.Companion.reverse

sealed interface LazyList<out A> {

    data class Cons<out A>( val head: () -> A, val tail: () -> LazyList<A>) : LazyList<A>
    object Nil : LazyList<Nothing>

    companion object {

        fun <A> cons(h : () -> A, t: () -> LazyList<A>): LazyList<A> {
            val head : A by lazy (h)
            val tail : LazyList<A> by lazy (t)
            return Cons( {head}, {tail})
        }

        fun <A> empty() : LazyList<A> = Nil
        fun <A> pure(a: A) :  LazyList<A> = Cons({ a }, { Nil })
        fun <A> of(vararg xs: A) : LazyList<A> =
            if(xs.isEmpty()) empty()
            else cons( { xs[0] }, { of(*xs.sliceArray(1 until xs.size) )} )

        ////////////////////////////////////////////////////////////////////////////////
        fun <A> LazyList<A>.toSList() : SList<A> {
            tailrec fun go(cur: LazyList<A>, acc: SList<A>): SList<A> =
                when(cur) {
                    is Nil -> acc
                    is Cons -> go( cur.tail(), SList.cons( cur.head(), acc))
                }

            return  go( this, SList.empty()).reverse()
        }

        fun <A> LazyList<A>.take(n: Int): LazyList<A> =
            when(this) {
                is Nil -> empty()
                is Cons ->
                    if( n <= 0 ) empty()
                    else cons(head) { tail().take(n-1)}
            }

        tailrec fun <A> LazyList<A>.drop(n: Int): LazyList<A> =
            when(this) {
                is Nil -> empty()
                is Cons ->
                    if(n <= 0 ) this
                    else tail().drop(n -1)
            }

        fun <A> LazyList<A>.takeWhile(p: (A) -> Boolean): LazyList<A> =
            when(this) {
                is Nil -> empty()
                is Cons ->
                    if(!p(head())) empty()
                    else cons( head) { tail().takeWhile(p) }
            }

        tailrec fun <A> LazyList<A>.exists(p: (A) -> Boolean): Boolean =
            when(this) {
                is Nil -> false
                is Cons -> if( p(head()) ) true else tail().exists(p)
            }

        fun <A,B> LazyList<A>.foldRight(base: () -> B,
                                        f: (A, () -> B) -> B): B =
            when(this) {
                is Nil -> base()
                is Cons -> f( head()) { tail().foldRight(base, f) }
            }

        fun <A,B> LazyList<A>.foldLeft(base: B, f: (B, A) -> B): B {

            tailrec fun go(cur: LazyList<A>, acc: B): B =
                when(cur) {
                    is Nil -> acc
                    is Cons -> go( cur.tail(), f(acc, cur.head()))
                }
            return go( this, base)
        }

        fun <A> LazyList<A>.forAll(p: (A) -> Boolean): Boolean =
            foldLeft( true ){ b, a -> p(a) && b }

        fun <A> infinite(a : A): LazyList<A> =
            cons( {a}, { infinite(a) })

        fun from(n: Int): LazyList<Int> =
            cons( {n} , {from(n +1)})

        fun fib(): LazyList<Int> {
            fun go(i: Int, j: Int): LazyList<Int> = cons( {i}, {go( j, i + j)})
            return go(0, 1)
        }

    }

    object Unfold {

        // generate infinite stream with seed & generating function
        fun <A,S> unfold(z: S, f: (S) -> Maybe<Pair<A,S>>): LazyList<A> =
            f(z).map { p ->
                cons( { p.first }, { unfold(p.second, f) })
            }.getOrElse {
                empty()
            }

        val fib =
            unfold( 0 to 1) { (current, next) ->
                just( current to (next to (current + next)))
            }

        fun from(n: Int): LazyList<Int> =
            unfold(n){ n -> just( n to (n+1)) }

        fun <A> infinite(a : A): LazyList<A> =
            unfold(a){ a -> just( a to a)}

        fun <A,B>LazyList<A>.map(f: (A) -> B) =
            unfold(this){ l ->
                when(l) {
                    is Nil -> none()
                    is Cons -> just( f(l.head()) to l.tail())
                }
            }

        fun <A> LazyList<A>.take(n : Int): LazyList<A> =
            unfold(this) { l ->
                when(l) {
                    is Nil -> none()
                    is Cons ->
                        if( n > 0) just( l.head() to l.tail().take(n -1))
                        else none()
                }
            }

        fun <A> LazyList<A>.takeWhile(p : (A) -> Boolean): LazyList<A> =
            unfold(this){ l ->
                when(l) {
                    is Nil -> none()
                    is Cons ->
                        if( p(l.head())) just( l.head() to l.tail())
                        else none()
                }
            }

        fun <A,B,C> LazyList<A>.zipWith( other: LazyList<B>, f:(A,B) -> C): LazyList<C> =
            unfold(this to other){ (l, o) ->
                when(l) {
                    is Nil -> none()
                    is Cons ->
                        if(o is Cons)
                            just( f(l.head(), o.head()) to (l.tail() to o.tail()))
                        else none()
                }
            }

        // pattern matching of kotlin is not good ;)
        fun <A,B> LazyList<A>.zipAll( other: LazyList<B>) : LazyList<Pair<Maybe<A>, Maybe<B>>> =
            unfold( this to other) { (l, o) ->
                when(l) {
                    is Nil -> when(o) {
                        is Nil -> None
                        is Cons -> {
                            val current = None to just(o.head() )
                            val seed = Nil to o.tail()
                            just( current to seed )
                        }
                    }
                    is Cons -> when(o) {
                        is Nil -> {
                            val current = just(l.head()) to None
                            val seed = l.tail() to Nil
                            just( current to seed )
                        }
                        is Cons -> {
                            val current = None to just(o.head() )
                            val seed = Nil to o.tail()
                            just( current to seed )
                        }
                    }
                }
            }

        fun <A> LazyList<A>.startWith( other: LazyList<A>) : Boolean =
            this.zipAll(other)
                .takeWhile{ !it.second.isEmpty}
                .forAll{ it.first == it.second }

        fun <A> LazyList<A>.tails(): LazyList<LazyList<A>> =
            unfold(this){ l ->
                when(l) {
                    is Nil -> None
                    is Cons -> just( l to l.tail())
                }
            }

        // todo
        // fun <A,B> LazyList<A>.scanRight(z: B, f: (A, () -> B) -> B) : LazyList<B> =

    }
}
