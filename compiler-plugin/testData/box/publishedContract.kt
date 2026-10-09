// WITH_STDLIB

// MODULE: lib
// FILE: contract.kt
package published

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Contract {
    val name: String
    fun text(prefix: String): String = prefix + name
}

// MODULE: main(lib)
// FILE: usage.kt
package usage

import published.Contract
import published.ContractImpl
import published.copy

fun box(): String {
    val contract = Contract("name")
    check(contract is ContractImpl)
    check(contract.text("factory:") == "factory:name")
    check(contract.copy().text("copy:") == "copy:name")
    check(contract.copy(name = "changed").name == "changed")
    return "OK"
}
