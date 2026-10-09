// WITH_STDLIB

package values

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Values {
    val flag: Boolean
    val byte: Byte
    val short: Short
    val int: Int
    val long: Long
    val char: Char
    val float: Float
    val double: Double
    val nullable: Int?
    val list: List<String>
    val array: IntArray
}

fun box(): String {
    val array = intArrayOf(1, 2)
    val value = Values(true, 1, 2, 3, 4, 'x', Float.NaN, Double.NaN, null, listOf("a"), array)
    check(value == value.copy())
    check(value.hashCode() == value.copy().hashCode())
    check(value != value.copy(array = intArrayOf(1, 2)))
    check(value != value.copy(nullable = 0))
    val positiveZero = value.copy(float = 0.0f, double = 0.0)
    val negativeZero = value.copy(float = -0.0f, double = -0.0)
    if (positiveZero == negativeZero) check(positiveZero.hashCode() == negativeZero.hashCode())
    return "OK"
}
