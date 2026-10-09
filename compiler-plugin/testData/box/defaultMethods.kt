// WITH_STDLIB

package defaults

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Formatter {
    fun first(value: Int): String
    val prefix: String
    fun second(value: Int = 4): String = prefix + first(value)
    fun fromDefault(): String = second()
    fun defaults(a: Int = prefix.length, b: Int = a + 1): String = "$a:$b"
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
}

@GenerateContract
interface Getter {
    val value: Int get() = 100
    fun constant() = 42
    fun read(): Int = value
}

fun box(): String {
    val formatter = Formatter(prefix = "prefix:", first = { "value:$it" })
    check(formatter.second() == "prefix:value:4")
    check(formatter.fromDefault() == "prefix:value:4")
    check(formatter.fromParameters() == "7:8")
    check(formatter.namedOrder() == "2:1")
    check(formatter.twice(3) == "prefix:value:3, prefix:value:3")
    check(formatter.arguments(1, 2, 3) == 6)
    check(formatter.copy().arguments(4, 5) == 9)
    check(Getter(value = 7).read() == 7)
    check(Getter(value = 7).constant() == 42)
    return "OK"
}
