// WITH_STDLIB

package versions

import com.alexcawl.contract.GenerateContract
import com.alexcawl.contract.IntroducedAtContract as Added

private const val FIRST_VERSION = 2

@GenerateContract
interface Appearance {
    @Added(version = FIRST_VERSION + 5)
    fun newest(prefix: String): String = prefix + (first() + second() + third() + last())
    fun first(): Int
    @Added(FIRST_VERSION)
    fun second(): Int = first() + 1
    fun third(): Int = 3
    @Added(7)
    fun last(): Int = 5
}

@GenerateContract
interface OnlyVersioned {
    @Added(1)
    fun first(): Int = 1
    @Added(1)
    fun second(): Int = 2
}

@GenerateContract
interface Unversioned {
    fun first(): Int
    fun second(): Int = 2
    fun third(): Int = 3
}

fun box(): String {
    val factories = Class.forName("versions.AppearanceConstructorKt").declaredMethods.filter { it.name == "Appearance" }
    val copies = Class.forName("versions.AppearanceCopyKt").declaredMethods.filter { it.name == "copy" }
    val constructors = Appearance.Contract::class.java.declaredConstructors.filter {
        it.parameterTypes.lastOrNull()?.simpleName != "DefaultConstructorMarker"
    }
    check(factories.map { it.parameterCount }.sorted() == listOf(2, 3, 5))
    check(copies.map { it.parameterCount }.sorted() == listOf(3, 4, 6))
    check(constructors.map { it.parameterCount }.sorted() == listOf(2, 3, 5))
    check(factories.filter { it.parameterCount < 5 }.all { it.isSynthetic })
    check(!factories.single { it.parameterCount == 5 }.isSynthetic)
    val first: Appearance.() -> Int = { 10 }
    val second: Appearance.() -> Int = { 50 }
    val third: Appearance.() -> Int = { 30 }
    val initial = factories.single { it.parameterCount == 2 }.invoke(null, first, third) as Appearance
    check(initial.newest("initial:") == "initial:56")
    val positional = Appearance(first, third)
    check(positional.newest("positional:") == "positional:56")
    val middle = factories.single { it.parameterCount == 3 }.invoke(null, first, third, second) as Appearance
    check(middle.newest("middle:") == "middle:95")
    check(Appearance(first, third, second).newest("middle:") == "middle:95")
    val positionalCallbacks = Appearance.Contract(first, third, second)
    check(positionalCallbacks.second!!(middle) == 50)
    check(positionalCallbacks.third!!(middle) == 30)
    val callbacks = constructors.single { it.parameterCount == 2 }.newInstance(first, third) as Appearance.Contract
    check(callbacks.first(initial) == 10)
    check(callbacks.third!!(initial) == 30)
    check(callbacks.second == null && callbacks.newest == null && callbacks.last == null)
    val current = Appearance(first = { 10 }, second = { first() + 100 }, last = { 99 }, newest = {
        it + (second() + third() + last())
    })
    val replacement: Appearance.() -> Int = { 20 }
    val copied = copies.single { it.parameterCount == 3 }.invoke(null, current, replacement, null) as Appearance
    check(copied.newest("copy:") == "copy:222")
    check(copied.contract().second === current.contract().second)
    check(copied.contract().newest === current.contract().newest)
    check(copied.contract().last === current.contract().last)
    val copiedPositional = current.copy(replacement, third)
    check(copiedPositional.newest("positional:") == "positional:249")
    check(copiedPositional.contract().second === current.contract().second)
    val emptyFactory = Class.forName("versions.OnlyVersionedConstructorKt").declaredMethods
        .filter { it.name == "OnlyVersioned" }
    check(emptyFactory.map { it.parameterCount }.sorted() == listOf(0, 2))
    val onlyVersioned = emptyFactory.single { it.parameterCount == 0 }.invoke(null) as OnlyVersioned
    check(onlyVersioned.first() == 1 && onlyVersioned.second() == 2)
    val emptyCallbacks = OnlyVersioned.Contract::class.java.getDeclaredConstructor().newInstance()
    check(emptyCallbacks.first == null && emptyCallbacks.second == null)
    val emptyCopy = Class.forName("versions.OnlyVersionedCopyKt").declaredMethods.single {
        it.name == "copy" && it.parameterCount == 1
    }
    val overridden = OnlyVersioned(first = { 9 })
    check((emptyCopy.invoke(null, overridden) as OnlyVersioned).first() == 9)
    check(Class.forName("versions.UnversionedConstructorKt").declaredMethods.count { it.name == "Unversioned" } == 1)
    check(Class.forName("versions.UnversionedCopyKt").declaredMethods.count { it.name == "copy" } == 1)
    check(Unversioned.Contract::class.java.declaredConstructors.none { it.parameterCount == 1 || it.parameterCount == 2 })
    return "OK"
}
