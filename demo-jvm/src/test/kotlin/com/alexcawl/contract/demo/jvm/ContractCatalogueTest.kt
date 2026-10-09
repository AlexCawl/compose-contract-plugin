package com.alexcawl.contract.demo.jvm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractCatalogueTest {
    private fun values(): ValueAppearance = ValueAppearance(
        emphasized = true,
        byteToken = 1,
        shortToken = 2,
        width = 48,
        color = 0xFF6750A4,
        marker = '*',
        opacity = Float.NaN,
        scale = Double.NaN,
        label = null,
        lineHeight = Pixels(24),
        optionalInset = null,
        decorations = mapOf("weekend" to listOf("accent", null)),
        stops = intArrayOf(0, 50, 100),
        labels = arrayOf("weekday", null),
        transform = { it.uppercase() },
        fallback = null,
    )

    @Test
    fun compoundValuesIncludeInlineNullableAndParameterizedTypes() {
        val original = values()
        val copied = original.copy(optionalInset = Pixels(8), fallback = original)

        assertTrue(original is ValueAppearanceImpl)
        assertTrue(original.emphasized)
        assertEquals(1.toByte(), original.byteToken)
        assertEquals(2.toShort(), original.shortToken)
        assertEquals(48, original.width)
        assertEquals(0xFF6750A4, original.color)
        assertEquals('*', original.marker)
        assertNull(original.label)
        assertEquals(Pixels(24), copied.lineHeight)
        assertEquals(Pixels(8), copied.optionalInset)
        assertEquals(original, copied.fallback)
        assertEquals(listOf("accent", null), copied.decorations["weekend"])
        assertEquals("DAY", copied.transform("day"))
    }

    @Test
    fun equalityUsesArrayAndFunctionIdentity() {
        val original = values()
        val copied = original.copy()

        assertEquals(original, copied)
        assertEquals(original.hashCode(), copied.hashCode())
        assertNotEquals(original, original.copy(stops = original.stops.copyOf()))
        assertNotEquals(original, original.copy(labels = original.labels.copyOf()))
        assertNotEquals(original, original.copy(transform = { it.uppercase() }))
        assertEquals(original, original.copy(transform = original.transform))
    }

    @Test
    fun boxedFloatingPointEqualityHandlesNaNAndSignedZero() {
        val original = values()

        assertEquals(original, original.copy(opacity = Float.NaN, scale = Double.NaN))
        assertNotEquals(original.copy(opacity = 0.0f), original.copy(opacity = -0.0f))
        assertNotEquals(original.copy(scale = 0.0), original.copy(scale = -0.0))
    }

    @Test
    fun defaultsUseSuppliedPropertiesEarlierCallbacksAndParameterDefaults() {
        val appearance = DefaultAppearance(prefix = "p:", spacing = 4, token = { "#$it" })

        assertEquals(4, appearance.spacing)
        assertEquals("p:#2", appearance.label())
        assertEquals("p:#2", appearance.summary())
        assertEquals("2:3", appearance.fromParameters())
        assertEquals("2:1", appearance.namedOrder())
        assertEquals("p:#3|p:#3", appearance.references(3))
        assertEquals(10, appearance.spacingSum(1, 2, 3))
        assertEquals("ap:b", appearance.join("a", "b"))
        assertEquals("p:#5", appearance.decorator()(5))
    }

    @Test
    fun laterDefaultsUseAnExplicitEarlierMethodOverride() {
        val appearance = DefaultAppearance(prefix = "p:", spacing = 4, token = { "#$it" }, label = { "custom:$it" })

        assertEquals("custom:2", appearance.summary())
        assertEquals("custom:3|custom:3", appearance.references(3))
        assertEquals("custom:5", appearance.decorator()(5))
    }

    @Test
    fun copyKeepsMethodsBoundToOriginalProperties() {
        val original = DefaultAppearance(prefix = "p:", spacing = 4, token = { "#$it" })
        val copied = original.copy(prefix = "new:", spacing = 99)

        assertEquals("new:", copied.prefix)
        assertEquals(99, copied.spacing)
        assertEquals("p:#1", copied.label(1))
        assertEquals(7, copied.spacingSum(1, 2))
        assertNotEquals(original, original.copy())
        assertEquals("custom:1", copied.copy(label = { "custom:$it" }).label(1))
    }

    @Test
    fun copyAcceptsManualImplementations() {
        val manual = object : JvmAppearance {
            override val width = 12
        }
        val copied = manual.copy(width = 24)

        assertTrue(copied is JvmAppearanceImpl)
        assertEquals(24, copied.width)
        assertEquals("width:12", copied.label())
    }

    @Test
    fun emptyAndInternalContractsGenerateValueImplementations() {
        assertEquals(EmptyAppearanceImpl(), EmptyAppearance().copy())
        assertEquals(EmptyAppearanceImpl().hashCode(), EmptyAppearance().hashCode())
        assertEquals(8, InternalAppearance(inset = 4).copy(inset = 8).inset)
    }

    @Test
    fun wideCopyPreservesFieldsAcrossAllThreeDefaultMasks() {
        val original = WidePalette.default
        val copied = original.copy(tone01 = 101, tone32 = 132, tone33 = 133, tone64 = 164, tone65 = 165)

        assertEquals(original, original.copy())
        assertEquals(original.hashCode(), original.copy().hashCode())
        assertEquals(101, copied.tone01)
        assertEquals(31, copied.tone31)
        assertEquals(132, copied.tone32)
        assertEquals(133, copied.tone33)
        assertEquals(34, copied.tone34)
        assertEquals(63, copied.tone63)
        assertEquals(164, copied.tone64)
        assertEquals(165, copied.tone65)
        assertEquals(65, original.tone65)
    }
}
