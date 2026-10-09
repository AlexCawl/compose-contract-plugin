// WITH_STDLIB

package interop

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface JavaContract {
    val value: Int
    fun format(prefix: String): String = prefix + value
}

fun box(): String {
    val facade = Class.forName("interop.JavaContractContractKt")
    val factory = facade.getMethod("JavaContract", Int::class.javaPrimitiveType)
    val contract = factory.invoke(null, 7) as JavaContract
    check(contract.format("java:") == "java:7")
    val copy = facade.getMethod("copy", JavaContract::class.java)
    val copied = copy.invoke(null, contract) as JavaContract
    check(copied.value == 7)
    check(copied.format("copy:") == "copy:7")
    check(facade.methods.count { it.name == "JavaContract" } == 2)
    check(facade.methods.count { it.name == "copy" } == 3)
    return "OK"
}
