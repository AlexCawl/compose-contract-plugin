// WITH_STDLIB

package sample

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface Appearance {
    fun a(): Int
    fun b(f: Boolean): Int
    fun c(f: Boolean): Int = if (f) 1 else 2
    fun d(): Int = 4
    fun e(f: Boolean): Int = c(f) * d()
}

class AppearanceContract

@GenerateContract
interface Empty

@GenerateContract
interface Optional {
    fun answer(): Int = 42
}

fun box(): String {
    val first: Appearance.() -> Int = { 1 }
    val second: Appearance.(Boolean) -> Int = { if (it) a() else 2 }
    val original = Appearance(a = first, b = second)
    val container = Appearance.Contract(a = first, b = second)
    check(container.a(original) == 1)
    check(container.b(original, true) == 1)
    check(container.c == null && container.d == null && container.e == null)
    check(AppearanceContract::class.java.enclosingClass == null)
    check(Appearance.Contract::class.java.enclosingClass == Appearance::class.java)
    check(java.lang.reflect.Modifier.isStatic(Appearance.Contract::class.java.modifiers))
    check(Empty.Contract::class.java.enclosingClass == Empty::class.java)
    check(Optional.Contract().answer == null)
    check(runCatching { Class.forName("sample.OptionalContract") }.exceptionOrNull() is ClassNotFoundException)
    check(original.e(true) == 4)
    val copied = original.copy(a = { 7 }, c = { 3 })
    check(copied.b(true) == 7)
    check(copied.e(true) == 12)
    check(copied.copy(d = { 5 }).e(true) == 15)
    val dependent = original.copy(e = { selected -> c(selected) * d() })
    check(dependent.copy(c = { 3 }).e(true) == 12)
    check(copied.copy(c = null).e(true) == 12)
    val equal = original.copy()
    check(original !== equal)
    check(original == equal)
    check(original.hashCode() == equal.hashCode())
    check(original == Appearance(first, second))
    check(original != copied)
    check(!original.equals(null))
    check(!original.equals("other"))
    check(original.contract() !== original.contract())
    check(Appearance.Contract::class.java.declaredMethods.none { it.name == "equals" || it.name == "hashCode" })
    var reads = 0
    val manual = object : Appearance {
        override fun a() = 5
        override fun b(f: Boolean) = if (f) a() else 0
        override fun contract(): Appearance.Contract {
            reads++
            return Appearance.Contract(a = { 5 }, b = { selected -> if (selected) a() else 0 })
        }
    }
    check(manual.copy(a = { 9 }).b(true) == 9)
    check(reads == 1)
    var seen: Appearance? = null
    val observing = original.copy(a = { seen = this; 1 }).copy()
    observing.a()
    check(seen === observing)
    check(Empty() == Empty().copy())
    check(Empty().hashCode() == Empty().copy().hashCode())
    check(Optional().answer() == 42)
    check(Optional().copy(answer = { 7 }).answer() == 7)
    return "OK"
}
