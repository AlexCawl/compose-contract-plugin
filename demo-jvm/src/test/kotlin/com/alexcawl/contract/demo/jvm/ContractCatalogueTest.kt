package com.alexcawl.contract.demo.jvm

import org.junit.Assert.*
import org.junit.Test

class ContractCatalogueTest {
    private fun values(): ValueAppearance = ValueAppearance(
        emphasized = { true }, byteToken = { 1 }, shortToken = { 2 }, width = { 48 },
        color = { 0xFF6750A4 }, marker = { '*' }, opacity = { Float.NaN }, scale = { Double.NaN },
        label = { null }, lineHeight = { Pixels(24) }, optionalInset = { null },
        decorations = { mapOf("weekend" to listOf("accent", null)) }, stops = { intArrayOf(0, 50, 100) },
        labels = { arrayOf("weekday", null) }, transform = { { it.uppercase() } }, fallback = { null },
    )

    @Test
    fun compoundCallbacksIncludeInlineNullableAndParameterizedTypes() {
        val original = values()
        val copied = original.copy(optionalInset = { Pixels(8) }, fallback = { original })
        assertTrue(original.emphasized())
        assertEquals(1.toByte(), original.byteToken())
        assertEquals(2.toShort(), original.shortToken())
        assertEquals(48, original.width())
        assertEquals(0xFF6750A4, original.color())
        assertEquals('*', original.marker())
        assertNull(original.label())
        assertEquals(Pixels(24), copied.lineHeight())
        assertEquals(Pixels(8), copied.optionalInset())
        assertEquals(original, copied.fallback())
        assertEquals(listOf("accent", null), copied.decorations()["weekend"])
        assertEquals("DAY", copied.transform()("day"))
    }

    @Test
    fun equalityComparesCallbacksWithoutEvaluatingThem() {
        val original = values()
        val copied = original.copy()
        assertEquals(original, copied)
        assertEquals(original.hashCode(), copied.hashCode())
        assertNotSame(original, copied)
        assertNotEquals(original, original.copy(width = { 48 }))
        assertEquals(original, original.copy(width = original.contract().width))
        assertNotSame(original.contract(), original.contract())
        assertTrue(ValueAppearance.Contract::class.java.declaredMethods.none { it.name == "equals" || it.name == "hashCode" })
    }

    @Test
    fun defaultsUseCurrentReceiverParameterDefaultsAndCallableReferences() {
        val original = DefaultAppearance(prefix = { "p:" }, spacing = { 4 }, token = { "#$it" })
        assertEquals("p:#2", original.summary())
        assertEquals("2:3", original.fromParameters())
        assertEquals("2:1", original.namedOrder())
        assertEquals("p:#3|p:#3", original.references(3))
        assertEquals(10, original.spacingSum(1, 2, 3))
        assertEquals("ap:b", original.join("a", "b"))
        assertEquals("p:#5", original.decorator()(5))
        val copied = original.copy(prefix = { "new:" }, spacing = { 99 })
        assertEquals("new:#1", copied.label(1))
        assertEquals(102, copied.spacingSum(1, 2))
        assertEquals("new:#4", copied.summary())
        assertEquals("custom:1", copied.copy(label = { "custom:$it" }).label(1))
        assertEquals(original, original.copy())
    }

    @Test
    fun copyAcceptsManualImplementationsWithExplicitContract() {
        val manual = object : JvmAppearance {
            override fun width() = 12
            override fun contract() = JvmAppearance.Contract(width = { 12 })
        }
        val copied = manual.copy(width = { 24 })
        assertEquals(24, copied.width())
        assertEquals("width:24", copied.label())
    }

    @Test
    fun emptyAndInternalContractsGenerateValueImplementations() {
        assertEquals(InternalAppearance::class.java, InternalAppearance.Contract::class.java.enclosingClass)
        assertEquals(EmptyAppearance(), EmptyAppearance().copy())
        assertEquals(EmptyAppearance().hashCode(), EmptyAppearance().hashCode())
        assertEquals(8, InternalAppearance(inset = { 4 }).copy(inset = { 8 }).inset())
    }

    @Test
    fun wideCopyPreservesCallbacksAcrossAllThreeDefaultMasks() {
        val original = WidePalette.default
        val copied = original.copy(tone01 = { 101 }, tone32 = { 132 }, tone33 = { 133 }, tone64 = { 164 }, tone65 = { 165 })
        assertEquals(original, original.copy())
        assertEquals(original.hashCode(), original.copy().hashCode())
        assertEquals(101, copied.tone01())
        assertEquals(31, copied.tone31())
        assertEquals(132, copied.tone32())
        assertEquals(133, copied.tone33())
        assertEquals(34, copied.tone34())
        assertEquals(63, copied.tone63())
        assertEquals(164, copied.tone64())
        assertEquals(165, copied.tone65())
        assertEquals(65, original.tone65())
    }
}
