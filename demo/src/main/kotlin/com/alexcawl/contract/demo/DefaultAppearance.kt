package com.alexcawl.contract.demo

import com.alexcawl.contract.GenerateContract
import com.alexcawl.contract.IntroducedAtContract

@GenerateContract
interface DefaultAppearance {
    fun token(index: Int): String
    fun prefix(): String
    // An omitted callback invokes this interface default on the current instance.
    @IntroducedAtContract(1)
    fun spacing(): Int = 8

    @IntroducedAtContract(1)
    fun label(index: Int = prefix().length): String = this.prefix() + token(index)
    @IntroducedAtContract(2)
    fun summary(): String = label()
    fun parameters(first: Int = prefix().length, second: Int = first + 1): String = "$first:$second"
    fun fromParameters(): String = parameters()
    fun order(first: Int, second: Int): String = "$first:$second"
    fun namedOrder(): String {
        var position = 0
        return order(second = ++position, first = ++position)
    }

    fun references(index: Int): String {
        val formatter = ::label
        return List(2) { formatter(index) }.joinToString("|")
    }

    fun spacingSum(vararg values: Int): Int = spacing() + values.sum()
    fun join(vararg values: String): String = values.joinToString(prefix())
    fun decorator(): (Int) -> String = { index -> label(index) }
}

// Required callbacks must be supplied to both the factory and the callback container.
// val missingPrefix = DefaultAppearance(token = { "#$it" })
// val missingToken = DefaultAppearance.Contract(prefix = { "day:" })
