// WITH_STDLIB

package values

import com.alexcawl.contract.GenerateContract

@JvmInline
value class Pixels(val value: Int)

@GenerateContract
interface Values {
    fun lineHeight(): Pixels
    fun nullable(): Pixels?
    fun list(): List<String>
    fun array(): IntArray
    fun transform(): (String) -> String
    fun float(): Float = Float.NaN
}

fun box(): String {
    val array = intArrayOf(1, 2)
    val height: Values.() -> Pixels = { Pixels(24) }
    val optional: Values.() -> Pixels? = { null }
    val value = Values(lineHeight = height, nullable = optional, list = { listOf("a") }, array = { array }, transform = { { it.uppercase() } })
    check(value == value.copy())
    check(value.hashCode() == value.copy().hashCode())
    check(value.lineHeight() == Pixels(24))
    check(value.nullable() == null)
    check(value.array() === array)
    check(value.transform()("day") == "DAY")
    check(value.float().isNaN())
    check(value != value.copy(nullable = { Pixels(0) }))
    check(value != value.copy(lineHeight = { Pixels(24) }))
    check(value.copy(nullable = { Pixels(8) }).nullable() == Pixels(8))
    return "OK"
}
