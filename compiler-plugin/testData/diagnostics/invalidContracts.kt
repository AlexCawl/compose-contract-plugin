// RUN_PIPELINE_TILL: FRONTEND

package invalid

import com.alexcawl.contract.GenerateContract

<!UNSUPPORTED!>@GenerateContract<!>
class NotAnInterface

<!UNSUPPORTED!>@GenerateContract<!>
interface Generic<T> {
    fun value(): T
}

<!UNSUPPORTED!>@GenerateContract<!>
interface Mutable {
    var value: Int
}

<!UNSUPPORTED!>@GenerateContract<!>
interface Property {
    val value: Int
}

interface Base {
    fun value(): Int
}

<!UNSUPPORTED!>@GenerateContract<!>
interface Inherited : Base

<!UNSUPPORTED!>@GenerateContract<!>
interface Overloaded {
    fun value(): Int
    fun value(input: Int): Int
}

<!UNSUPPORTED!>@GenerateContract<!>
interface Reserved {
    fun contract(): Int
}

<!UNSUPPORTED!>@GenerateContract<!>
interface GetterCollision {
    fun foo(): Int
    fun Foo(): Int
}

<!UNSUPPORTED!>@GenerateContract<!>
interface ContainerCollision {
    fun value(): Int
    class Contract
}

<!UNSUPPORTED!>@GenerateContract<!>
interface ObjectContainerCollision {
    fun value(): Int
    object Contract
}

<!UNSUPPORTED!>@GenerateContract<!>
interface InterfaceContainerCollision {
    fun value(): Int
    interface Contract
}

fun collisionHasNoGeneratedMembers(contract: ContainerCollision.Contract) {
    contract.<!UNRESOLVED_REFERENCE!>value<!>
}

<!UNSUPPORTED!>@GenerateContract<!>
interface ImplementationCollision { fun value(): Int }
class ImplementationCollisionImpl

<!UNSUPPORTED!>@GenerateContract<!>
interface ConstructorFacadeCollision { fun value(): Int }
class ConstructorFacadeCollisionConstructorKt

<!UNSUPPORTED!>@GenerateContract<!>
interface CopyFacadeCollision { fun value(): Int }
class CopyFacadeCollisionCopyKt

@GenerateContract
interface Required {
    fun value(): Int
    fun optional(): Int = 0
}

fun missing() {
    <!NO_VALUE_FOR_PARAMETER!>Required<!>()
    Required.<!NO_VALUE_FOR_PARAMETER!>Contract<!>()
}
