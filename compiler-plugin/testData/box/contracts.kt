// WITH_STDLIB

package sample

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Appearance {
    val width: Int
    val label: String?
    fun onClick(value: Int): String
    fun describe(prefix: String): String = "$prefix$label:$width"
}

@GenerateContract
interface Size {
    val width: Int
    val height: Int
}

@GenerateContract
interface Empty

fun box(): String {
    val click: (Int) -> String = { "click:$it" }
    val description: (String) -> String = { "description:$it" }
    val a = Appearance(10, null, click, description)
    val b = AppearanceImpl(10, null, click, description)
    check(a == b)
    check(a.hashCode() == b.hashCode())
    check(a != Appearance(11, null, click, description))
    check(a != Appearance(10, "label", click, description))
    check(a != Appearance(10, null, { "other:$it" }, description))
    check(!a.equals(null))
    check(!a.equals("other"))
    check(a.onClick(2) == "click:2")
    check(a.describe("test") == "description:test")
    val defaults = Appearance(20, "name", click)
    check(defaults.describe("test:") == "test:name:20")
    val copy = defaults.copy(width = 30)
    check(copy.width == 30)
    check(copy.label == "name")
    check(copy.onClick(3) == "click:3")
    check(copy.describe("copy:") == "copy:name:20")
    check(copy != defaults)
    check(a.copy(onClick = click, describe = description) == a)
    val custom = object : Appearance {
        override val width = 5
        override val label = "custom"
        override fun onClick(value: Int): String = "$label:$value"
    }
    check(custom.copy().describe("test:") == "test:custom:5")
    val size = Size(width = 1, height = 2)
    check(size.copy() == size)
    check(size.copy(height = 3) == SizeImpl(1, 3))
    check(Empty() == EmptyImpl())
    check(Empty().hashCode() == EmptyImpl().hashCode())
    return "OK"
}
