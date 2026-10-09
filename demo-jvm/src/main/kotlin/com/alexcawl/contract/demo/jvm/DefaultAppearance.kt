package com.alexcawl.contract.demo.jvm

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface DefaultAppearance {
    fun token(index: Int): String
    val prefix: String
    // A getter does not supply a factory default: callers must still pass spacing.
    val spacing: Int get() = 8

    fun label(index: Int = prefix.length): String = this.prefix + token(index)
    fun summary(): String = label()
    fun parameters(first: Int = prefix.length, second: Int = first + 1): String = "$first:$second"
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

    fun spacingSum(vararg values: Int): Int = spacing + values.sum()
    fun join(vararg values: String): String = values.joinToString(prefix)
    fun decorator(): (Int) -> String = { index -> label(index) }
}
