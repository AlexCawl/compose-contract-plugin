// WITH_STDLIB

package defaults

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Formatter {
    fun first(value: Int): String
    fun prefix(): String
    fun second(value: Int = 4): String = prefix() + first(value)
    fun fromDefault(): String = second()
    fun defaults(a: Int = prefix().length, b: Int = a + 1): String = "$a:$b"
    fun fromParameters(): String = defaults()
    fun order(first: Int, second: Int): String = "$first:$second"
    fun namedOrder(): String {
        var value = 0
        return order(second = ++value, first = ++value)
    }
    fun twice(value: Int): String {
        val formatter = ::second
        return (1..2).joinToString { formatter(value) }
    }
    fun arguments(vararg values: Int): Int = values.sum()
    fun forward(): Int = later()
    fun later(): Int = 42
    fun recursive(depth: Int): Int = if (depth == 0) later() else recursive(depth - 1)
    fun identity(): Any = this
    fun argument(value: Any = this): Any = value
    fun nullable(): String? = "default"
}

@GenerateContract
interface Interleaved {
    fun first(): Int = last()
    fun last(): Int
}

fun box(): String {
    val formatter = Formatter(prefix = { "prefix:" }, first = { "value:$it" })
    check(formatter.second() == "prefix:value:4")
    check(formatter.fromDefault() == "prefix:value:4")
    check(formatter.fromParameters() == "7:8")
    check(formatter.namedOrder() == "2:1")
    check(formatter.twice(3) == "prefix:value:3, prefix:value:3")
    check(formatter.arguments(1, 2, 3) == 6)
    val copied = formatter.copy(prefix = { "new:" }, later = { 9 }, nullable = { null })
    check(copied.fromDefault() == "new:value:4")
    check(copied.forward() == 9)
    check(copied.recursive(2) == 9)
    check(copied.identity() === copied)
    check(copied.argument() === copied)
    check(copied.nullable() == null)
    check(copied.copy().nullable() == null)
    check(copied.copy().arguments(4, 5) == 9)
    check(Interleaved(last = { 7 }).first() == 7)
    check(Interleaved(last = { 7 }).copy(last = { 8 }).first() == 8)
    return "OK"
}
