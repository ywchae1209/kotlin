package parallel

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

interface Strategy {

    operator fun <A> invoke( a: () -> A): (Unit) -> A

    ////////////////////////////////////////////////////////////////////////////////
    companion object {

        fun from(es: ExecutorService): Strategy =
            object : Strategy {
                override fun <A> invoke(a: () -> A): (Unit) -> A {
                    val fa = es.submit(Callable<A> { a() })
                    return { fa.get() }
                }
            }

        fun sequential(): Strategy =
            object : Strategy {
                override fun <A> invoke(a: () -> A): (Unit) -> A =
                    { a() }
            }
    }
}

/**
 *
 * note::
 * node's get/set == get/set node's reference
 *
 */
class Node<A>( var value: A? = null) : AtomicReference<Node<A>>()

/**
 * see: MPMC (Multiple Produce Single Consuming
 *
 */
class Actor<A> ( val strategy: Strategy,
                 val onError: (Throwable) -> Unit = { throw it },
                 val handler: (A) -> Unit ) {

    private val tail = AtomicReference( Node<A>())      // consuming point (first ref is Node(null))
    private val head = AtomicReference( tail.get())     // last inserted point
    private val suspend = AtomicBoolean(true)
    private val bulk = 1024


    /**
     * Non-blocking queue insertion (using AtomicReference)
     * note) AtomicReference.get/set call get(or set) head,tail,node's __reference__
     */
    fun send(a: A) {

        val n = Node(a)

        // head's ref == newly inserted node
        // previous-node's next == newly inserted node
        head.getAndSet(n).lazySet(n)

        trySchedule()
    }

    fun <B> contraMap(f: (B) -> A): Actor<B> =
        Actor(strategy){
                b: B -> this.send(f(b))
        }


    ////////////////////////////////////////////////////////////////////////////////

    private fun trySchedule() {
        if( suspend.compareAndSet(true, false))
            schedule()
    }

    // append new thread-work of act()
    private fun schedule() =
        strategy.invoke { act() }

    private fun act() {

        val t = tail.get()          // skip first ref ( Node(null) )
        val n = batch( t, bulk)     // n is already processed in batch

        if( n != t) {               // more than 1 is processed ( may remain something..)

            n.value = null          // set last processed node's value to null
            tail.lazySet(n)

            schedule()              // append new real-thread work of act()

        } else {
            suspend.set(true)
            if( n.get() != null )   // try again if some nodes appended while batch
                trySchedule()
        }
    }

    private tailrec fun batch( t: Node<A>, i: Int) : Node<A> {

        val n = t.get()
        return if( n != null) {
            try {
                handler( n.value!!)
            } catch(e: Throwable) {
                onError(e)
            }
            if(i > 0) batch( n, i - 1) else n   // n is already processed by handler
        } else
            t
    }
}

fun main() {

    val es: ExecutorService = Executors.newFixedThreadPool(3)
    val s = Strategy.from(es)

    val actor1 = Actor(s) {s: Pair<Int,Actor<Any>> ->
        val count = s.first + 2
        val msg = "\t\t++++ ${s.first}:$count"
        Thread.sleep(300)
        println(msg)
        s.second.send( count to s.second)
    }

    val actor2 = Actor(s) {s: Pair<Int,Actor<Any>> ->

        val count = s.first + 1
        val msg = ">>>> ${s.first}:$count"
        val msg1 =">>>> ${s.first}:${count+2}"
        Thread.sleep(500)
        println(msg)
        println(msg1)
        actor1.send( count to s.second)
        actor1.send( (count + 2) to s.second)
    }

    // coercing to Any for convenience reason... ;)
    actor2.send( 0 to (actor2 as Actor<Any>))
}