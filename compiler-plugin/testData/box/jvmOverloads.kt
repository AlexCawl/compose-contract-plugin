// WITH_STDLIB

package interop

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface JavaContract {
    fun value(): Int
    fun format(prefix: String): String = prefix + value()
}

fun box(): String {
    val constructor = Class.forName("interop.JavaContractConstructorKt")
    val copy = Class.forName("interop.JavaContractCopyKt")
    val factoryMethods = constructor.declaredMethods.filter { it.name == "JavaContract" }
    check(factoryMethods.size == 2)
    check(factoryMethods.single { it.parameterCount == 1 }.isSynthetic)
    check(!factoryMethods.single { it.parameterCount == 2 }.isSynthetic)
    val copyMethods = copy.declaredMethods.filter { it.name == "copy" }
    check(copyMethods.size == 2)
    check(copyMethods.single { it.parameterCount == 2 }.isSynthetic)
    check(!copyMethods.single { it.parameterCount == 3 }.isSynthetic)
    check(JavaContractContract::class.java.declaredConstructors.single { it.parameterCount == 1 }.isSynthetic)
    val contract = JavaContract(value = { 7 })
    check(contract.format("java:") == "java:7")
    check(contract.copy(value = { 9 }).format("copy:") == "copy:9")
    return "OK"
}
