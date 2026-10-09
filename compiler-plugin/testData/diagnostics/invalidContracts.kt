// RUN_PIPELINE_TILL: FRONTEND

package invalid

import com.alexcawl.contract.GenerateContract

<!UNSUPPORTED!>@GenerateContract<!>
class NotAnInterface

<!UNSUPPORTED!>@GenerateContract<!>
interface Generic<T> {
    val value: T
}

<!UNSUPPORTED!>@GenerateContract<!>
interface Mutable {
    var value: Int
}

interface Base {
    val value: Int
}

<!UNSUPPORTED!>@GenerateContract<!>
interface Inherited : Base

<!UNSUPPORTED!>@GenerateContract<!>
interface Overloaded {
    fun value(): Int
    fun value(input: Int): Int
}

@GenerateContract
interface ForwardDefault {
    fun first(): Int = <!UNSUPPORTED!>second()<!>
    fun second(): Int = 1
}

@GenerateContract
interface RecursiveDefault {
    fun first(): Int = <!UNSUPPORTED!>first()<!>
}

@GenerateContract
interface StandaloneThis {
    fun identity(): Any = <!UNSUPPORTED!>this<!>
}

@GenerateContract
interface PropertyReference {
    val value: Int
    fun reference(): () -> Int = <!UNSUPPORTED!>::value<!>
}

@GenerateContract
interface DefaultArgumentReceiver {
    fun identity(value: Any = <!UNSUPPORTED!>this<!>): Any = value
}
