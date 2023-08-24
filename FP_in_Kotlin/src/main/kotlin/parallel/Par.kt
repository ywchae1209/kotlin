package parallel

import collections.*
import collections.Maybe.Companion.fold
import collections.SList.Companion.append
import collections.SList.Companion.drop
import collections.SList.Companion.empty
import collections.SList.Companion.head
import collections.SList.Companion.isEmpty
import collections.SList.Companion.map
import collections.SList.Companion.size
import collections.SList.Companion.take
import parallel.NonBlocking.parMap
import parallel.NonBlocking.runWith
import java.lang.System.exit
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

object NonBlocking {

    interface Future<A> {
        operator fun invoke(callback: (A) -> Unit): Unit
    }

    data class Par<A>( val f: (ExecutorService) -> Future<A>) {
        operator fun invoke(es: ExecutorService): Future<A> = f(es)
    }

    ////////////////////////////////////////////////////////////////////////////////
    fun <A> run(es: ExecutorService, p: Par<A>): A {

        val ref = AtomicReference<A>()
        val latch = CountDownLatch(1)

        p(es).invoke{ a ->
            ref.set(a)
            latch.countDown()
        }
        latch.await()
        return ref.get()
    }
    fun <A> Par<A>.runWith(es: ExecutorService) = run(es, this)

    ////////////////////////////////////////////////////////////////////////////////
    fun <A> unit(a: A): Par<A> =
        Par { es ->
            object : Future<A> {
                override fun invoke(callback: (A) -> Unit) = callback(a)
            }
        }

    fun <A> lazyUnit( a: () -> A): Par<A> =
        Par { es ->
            object : Future<A> {
                override fun invoke(callback: (A) -> Unit) = callback(a())
            }
        }

    private fun submit(es: ExecutorService, proc: () -> Unit) {
        es.submit( Callable{ proc()})
    }

    fun <A> fork( a: () -> Par<A>) =
        Par { es ->
            object : Future<A> {
                override fun invoke(callback: (A) -> Unit) {
                    submit(es) { a()(es).invoke(callback) }
                }
            }
        }

    fun <A,B> async( f: (A) -> B): (A) -> Par<B> = { unit(f(it)) }

    fun <A, B, C> map2(pa: Par<A>, pb: Par<B>, f: (A, B) -> C): Par<C> =
        Par { es ->
            object : Future<C> {

                override fun invoke(cb: (C) -> Unit) {

                    val ar = AtomicReference<Maybe<A>>(None)
                    val br = AtomicReference<Maybe<B>>(None)

                    // Actor
                    val combiner = Actor<Either<A, B>>(Strategy.from(es)) { eab ->
                            when (eab) {
                                is Left<A> ->
                                    br.get().fold(
                                        { ar.set(Just(eab.get)) },
                                        { b -> submit(es) { cb(f(eab.get, b)) } }
                                    )
                                is Right<B> ->
                                    ar.get().fold(
                                        { br.set(Just(eab.get)) },
                                        { a -> submit(es) { cb(f(a, eab.get)) } }
                                    )
                            }
                        }
                    pa(es).invoke { a: A -> combiner.send(Left(a)) }
                    pb(es).invoke { b: B -> combiner.send(Right(b)) }
                }
            }
        }

    fun <A,B> map(pa: Par<A>, f: (A) -> B): Par<B> =
        map2( pa, unit {}){ a, _ -> f(a)}

    fun <A,B> flatMap(pa: Par<A>, f: (A) -> Par<B>) =
        Par { es ->
            object : Future<B> {
                override fun invoke(callback: (B) -> Unit) {
                    pa(es).invoke{ a ->
                        f(a).invoke(es)
                            .invoke(callback)
                    }
                }
            }
        }

    ////////////////////////////////////////////////////////////////////////////////
    // todo :: current SList is not efficient
    fun <A> sequence(lp: SList<Par<A>>): Par<SList<A>> =
        when {
            lp.isEmpty() -> unit(empty())
            lp.size() == 1 -> map(lp.head()){ println(it); SList.of(it) }
            else -> {
                val half = lp.size() /2
                val l =  lp.take(half)
                val r = lp.drop(half)
                map2(sequence(l), sequence(r)){la, lb -> la.append(lb)}
            }
        }

    fun <A,B> SList<A>.parMap( f: (A) -> B): Par<SList<B>> {

        val fs = this.map( async(f))
        return sequence( fs)
    }

}
fun main() {

    val p = SList.of(1,2,3,4,5,6,7,8,9).parMap {
        it to sqrt(it.toDouble())
    }.runWith(newFixedThreadPool(2))

    println(p)

    exit(0)
}
