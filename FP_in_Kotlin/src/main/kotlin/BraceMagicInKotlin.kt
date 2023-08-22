object BraceMagicInKotlin {

    // type of val ?
    ////////////////////////////////////////////////////////////////////////////////
    val i_1 = 1         // Int
    val i_2 = (1)       // Int () == single expression (which is evaluated)

    ////////////////////////////////////////////////////////////////////////////////
    data class Person(val name: String, val age: Int, val address: String, val isStudent: Boolean )
    val p = Person("John", 11, "Earth", true)

    // destructing declaration
    fun destructing_declaration(p: Person) {
        val (i,j,k,l) = p   // only-allowed in local variable.
    }

    ////////////////////////////////////////////////////////////////////////////////
    // meaning of { } block
    //  1. multiple expression, evaluated value is last expression.
    //  2. {} block is not evaluated (when it is on rhs )

    val i_3 = { println("1"); 1}     // () -> Int
    val i_5 = { a: Int -> a + 1}              // (Int) -> Int
    val i_4 = { { 1 } }                       // () -> () -> Int
    val i_6 = { a: Int -> { b: Int -> a * b}} // (Int) -> (Int) -> Int

    fun assigned_f1() =                       // () -> String
        {
            println("assigned function")
            "this is awesome"
        }

    fun assigned_f2() =
        run {             // String
            println("assigned function")
            "this is awesome"
        }

    fun assigned_f3() = {                      // String.
        println("assigned function")
        "this is awesome"
    }()                                        // ()

    fun not_assinged_f4() {                    // String : {} block is evaluated
        println("assigned function")
        "this is awesome"
    }

    fun f_when (age: Int) =
        when {
            age < 0 -> "not born yet."
            age < 10 -> "infant"
            age < 20 ->
                // {} block is evaluated to String(last expression)
            {
                println("something")
                "{} block's last expression"
            }
            else -> "other"
        }

    // {} block is evaluated
    fun f_if( age: Int) =
        if(age < 20)
        {
            println("")
            "below 20"
        }
        else
        {
            println("")
            "above 20"
        }
    // first {} block is evaluated, inner {} is not evaluated(lambda function)
    fun f_if2( age: Int) =          // () -> String
        if(age < 20)
        {
            {
                println("")
                "below 20"
            }

        }
        else
        {
            {
                println("")
                "above 20"
            }
        }
}