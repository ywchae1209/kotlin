package collections

sealed interface LazyList<out A> {

    data class Cons<out A>( val head: () -> A, val tail: () -> LazyList<A>) : LazyList<A>
    object Nil : LazyList<Nothing>

    companion object {

    }
}



