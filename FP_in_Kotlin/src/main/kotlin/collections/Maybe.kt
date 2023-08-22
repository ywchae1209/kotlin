package collections

import collections.SList.Companion.cons
import collections.SList.Companion.foldRight

sealed interface Maybe<out A> {

    companion object {
        fun <A> pure(a: A): Maybe<A> = Just(a)
        fun <A> none(): Maybe<A> = None

        fun <A,B> lift(f: (A) -> B): (Maybe<A>) -> Maybe<B> = {
            it.map(f)
        }
        
        fun <A> maybe(a: () -> A, logger: (String) -> Unit = {}): Maybe<A> =
            try {
                pure(a())
            } catch (e: Throwable) {
                logger(e.stackTraceToString())
                none()
            }

        ////////////////////////////////////////////////////////////////////////////////
        fun <A,B> Maybe<A>.fold(ifNone: () -> B, f: (A) -> B ) =
            when(this) {
                is None -> ifNone()
                is Just -> f(get)
            }

        fun <A,B> Maybe<A>.flatMap(f: (A) -> Maybe<B>): Maybe<B> =
            fold( { none() } ){ f(it) }

        fun <A,B> Maybe<A>.map(f: (A) -> B): Maybe<B> =
            flatMap{ pure(f(it)) }

        fun <A,B,C> map2(oa: Maybe<A>, ob: Maybe<B>, f: (A, B) -> C) =
            oa.flatMap { a ->
                ob.map { b ->
                    f(a, b)
                }
            }

        fun <A> Maybe<A>.getOrElse(alternative: () -> A): A =
            fold({ alternative()}){ it }

        fun <A> Maybe<A>.orElse(other: () -> Maybe<A>) =
            fold({ other() }){ this }

        fun <A> SList<Maybe<A>>.sequence(): Maybe<SList<A>> =
            traverse { it }

        fun <A,B> SList<A>.traverse(f: (A) -> Maybe<B>): Maybe<SList<B>> =
            foldRight( none()){ a, b -> map2(f(a), b) { c, d -> cons( c, d) } }
    }
}

data class Just<out A>(val get: A) : Maybe<A>
object None: Maybe<Nothing>
