package collections

import kotlin.math.max

sealed interface BTree<out A> {
    data class Leaf<out A>(val get: A): BTree<A>
    data class Branch<out A>(val left: BTree<A>, val right: BTree<A>): BTree<A>

    companion object {

        fun <A> leaf(a: A): BTree<A> = Leaf(a)

        // note:: not stack-safe
        fun <A,B> BTree<A>.fold(lf: (A) -> B, bf: (B, B) -> B): B =
            when(this) {
                is Leaf -> lf(get)
                is Branch -> bf( left.fold(lf, bf), right.fold(lf, bf))
            }


        fun <A,B> BTree<A>.map(f: (A) -> B): BTree<B> =
            when(this) {
                is Leaf -> Leaf(f(get))
                is Branch -> Branch( left.map(f), right.map(f) )
            }

        fun <A,B> BTree<A>.mapViaFold(f: (A) -> B): BTree<B> =
            fold({ leaf(f(it))}){ l, r -> Branch(l, r)}

        // extended property
        val <A> BTree<A>.depth : Int
            get() = fold({1}){ l, r -> max(l,r) + 1}

        // extended property
        val <A> BTree<A>.count : Int
            get() = fold({1}){l, r -> l + r + 1}
    }
}