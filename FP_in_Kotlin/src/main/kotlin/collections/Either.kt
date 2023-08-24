package collections

sealed interface Either<out L, out R>
data class Left<L>( val get: L) : Either<L, Nothing>
data class Right<R>( val get: R) : Either<Nothing, R>


////////////////////////////////////////////////////
fun <L,R> right( a: R) : Either<L,R> = Right(a)
fun <L,R> left( a: L) : Either<L,R> = Left(a)
fun <L,R> unit( a: R) = right<L,R>(a)


////////////////////////////////////////////////////

fun <L,A,B> Either<L,A>.flatMap( f: (A) -> Either<L,B>) =
    when(this) {
        is Right -> f(get)
        is Left -> Left(get)
    }

fun <L,A,B> Either<L,A>.map( f: (A) -> B) =
    this.flatMap{ unit(f(it)) }

fun <L,A,B,C> map( ea: Either<L,A>, eb: Either<L,B>, f: (A,B) -> C) =
    ea.flatMap { a ->
        eb.map{ b ->
            f(a,b)
        }
    }