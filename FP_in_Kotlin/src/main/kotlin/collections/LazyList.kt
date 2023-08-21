package collections

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
            tailrec fun go(acc: SList<A>, cur: LazyList<A>): SList<A> =
                when(cur) {
                    is Nil -> acc.reverse()
                    is Cons -> go(SList.cons( cur.head(), acc), cur.tail())
                }

            return  go( SList.empty(), this)
        }

        // take, drop, exist, foldRight
        fun <A> LazyList<A>.take(n: Int): LazyList<A> {

            fun go(acc: LazyList<A>, i: Int): LazyList<A> =
                when(acc) {
                    is Nil -> acc
                    is Cons ->
                        if(n <= 0) acc
                        else cons( acc.head) { go(acc.tail(), i - 1) }
                }
            return go(this, n)
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



    }
}



