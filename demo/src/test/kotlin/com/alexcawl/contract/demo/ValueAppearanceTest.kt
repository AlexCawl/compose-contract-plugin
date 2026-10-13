package com.alexcawl.contract.demo

import org.junit.Assert.*
import org.junit.Test

class ValueAppearanceTest {
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
    fun newArrayCallbackPreservesContentsButChangesEquality() {
        val original = values()
        val copied = original.copy(stops = { original.stops().copyOf() })
        assertArrayEquals(original.stops(), copied.stops())
        assertNotEquals(original, copied)
    }
}
