// WITH_STDLIB

// MODULE: lib
// FILE: contract.kt
package published

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Contract {
    fun name(): String
    fun text(prefix: String): String = prefix + name()
}

// MODULE: main(lib)
// FILE: usage.kt
package usage

import published.Contract
import published.ContractContract
import published.copy

fun box(): String {
    val contract: Contract = Contract(name = { "name" })
    check(contract.text("factory:") == "factory:name")
    check(contract.copy().text("copy:") == "copy:name")
    check(contract.copy(name = { "changed" }).text("copy:") == "copy:changed")
    val manual = object : Contract {
        override fun name() = "manual"
        override fun contract() = ContractContract(name = { "manual" })
    }
    check(manual.copy(name = { "changed" }).text("manual:") == "manual:changed")
    return "OK"
}
