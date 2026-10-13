package com.alexcawl.contract.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppearanceTest {
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
        assertEquals("new:", copied.prefix())
        assertEquals(99, copied.spacing())
        // Copied callbacks and default bodies use the new contract instance.
        assertEquals("new:#1", copied.label(1))
        assertEquals(102, copied.spacingSum(1, 2))
        assertEquals("new:#4", copied.summary())
        assertEquals("custom:1", copied.copy(label = { "custom:$it" }).label(1))
        assertEquals(original, original.copy())
    }
}
