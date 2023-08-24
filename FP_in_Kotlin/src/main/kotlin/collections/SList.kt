package collections

sealed interface SList<out A> {

    val isEmpty: Boolean

    data class Cons<out A>( val head: A, val tail: SList<A>) : SList<A> {
        override fun toString(): String = "($head, $tail)"
        override val isEmpty: Boolean = false
    }

    object Nil : SList<Nothing> {
        override fun toString(): String = "Nil"
        override val isEmpty: Boolean = true
    }

    companion object {

        // constructor
        ////////////////////////////////////////////////////////////////////////////////
        fun <A> pure(a: A): SList<A> = Cons(a, Nil)
        fun <A> empty(): SList<A> = Nil
        fun <A> cons(h: A, t: SList<A>): SList<A> = Cons(h, t)

        fun <A> of(vararg xs: A): SList<A> {

            tailrec fun go(acc: SList<A>, vararg va: A): SList<A> =
                if(va.isEmpty()) acc
                else go(cons( va[0], acc), *va.sliceArray(1 until va.size))

            return go(Nil, *xs).reverse()
        }

        ////////////////////////////////////////////////////////////////////////////////
        // utility functions
        ////////////////////////////////////////////////////////////////////////////////
        tailrec fun <A,B> SList<A>.foldLeft(base: B, f: (B, A) -> B): B =
            when(this) {
                is Nil -> base
                is Cons -> this.tail.foldLeft( f(base, this.head), f)
            }

        fun <A> SList<A>.head(): A = if( this is Cons) head else throw Exception("head on Nil")

        fun <A> SList<A>.isEmpty(): Boolean = (this is Nil)

        fun <A> SList<A>.size(): Int = foldLeft(0){b, _ -> b + 1}

        fun <A> SList<A>.reverse(): SList<A> = this.foldLeft( empty<A>()) { b, a -> cons(a, b) }

        fun <A,B> SList<A>.foldRight(base: B,f: (A, B) -> B): B =
            this.reverse().foldLeft(base){ b, a -> f(a, b)}

        fun <A,B> SList<A>.flatMap(f: (A) -> SList<B>): SList<B> =
            this.foldRight( { empty<B>()} ) {a, b -> { f(a).append(b()) }}()

        fun <A,B> SList<A>.map(f: (A) -> B) =
            this.flatMap{ pure(f(it)) }

        fun <A> SList<SList<A>>.concat(): SList<A> =
            this.flatMap{ it }

        fun <A> SList<A>.filter(p: (A) -> Boolean): SList<A> =
            foldRight(empty()){ a, b -> if(p(a)) cons(a, b) else b }

        fun <A> SList<A>.all(p: (A) -> Boolean): Boolean =
            foldLeft(true){ b, a -> p(a) && b }

        fun <A> SList<A>.any(p: (A) -> Boolean): Boolean =
            foldLeft(false){ b, a -> p(a) || b}

        ////////////////////////////////////////////////////////////////////////////////

        fun <A> fill(n: Int, a: A): SList<A> {

            fun go(acc: SList<A>, i: Int): SList<A> =
                if(i <= 0) acc
                else go( cons( a, acc), i - 1)

            return go(Nil, n)
        }

        fun <A,B,C> map2(la: SList<A>, lb: SList<B>, f: (A,B) -> C): SList<C> =
            la.flatMap{ a ->
                lb.map{ b ->
                    f(a,b)
                }
            }

        fun <A> SList<A>.take(n: Int): SList<A> {

            tailrec fun go(cur: SList<A>, acc: SList<A>, i: Int): SList<A> =
                when(cur) {
                    is Nil -> acc
                    is Cons ->
                        if( i <= 0 ) acc
                        else go( cur.tail, cons(cur.head, acc), i - 1)
                }
            return go(this, Nil, n).reverse()
        }

        fun <A> SList<A>.takeWhile(p: (A) -> Boolean): SList<A> {

            tailrec fun go(cur: SList<A>, acc: SList<A>): SList<A> =
                when(cur) {
                    is Nil -> acc
                    is Cons ->
                        if(! p(cur.head)) acc
                        else go( cur.tail, cons(cur.head,acc))
                }
            return go(this, Nil).reverse()
        }

        fun <A> SList<A>.isSorted(order: (A, A) -> Boolean): Boolean {
            tailrec fun go(x: A, xs: SList<A>): Boolean =
                when (xs) {
                    is Nil -> true
                    is Cons ->
                        if(order(x, xs.head))
                            go(xs.head, xs.tail)
                        else
                            false
                }
            return if (this is Cons) go(head, tail) else true
        }

        fun <A> SList<A>.append(o: SList<A>): SList<A> =
            when(this) {
                is Nil -> o
                is Cons -> if(o is Nil) this else cons( head, tail.append(o))
            }

        fun <A,B,C> SList<A>.zipWith(ol: SList<B>, f: (A,B) -> C): SList<C> {
            tailrec fun go(al: SList<A>, bl: SList<B>, acc: SList<C>): SList<C> =
                if (al is Cons && bl is Cons)
                    go( al.tail, bl.tail, cons(f(al.head, bl.head), acc))
                else
                    acc

            return go(this, ol, Nil).reverse()
        }

        tailrec fun <A> SList<A>.drop(n : Int): SList<A> =
            when(this) {
                is Nil -> empty()
                is Cons -> if(n <= 0) this else tail.drop(n-1)
            }

        tailrec fun <A> SList<A>.dropWhile(p: (A) -> Boolean): SList<A> =
            when(this) {
                is Nil -> empty()
                is Cons -> if(!p(head)) this else tail.dropWhile(p)
            }
    }
}

