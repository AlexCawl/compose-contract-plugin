package com.alexcawl.contract.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class EmptyAppearanceTest {
    @Test
    fun emptyContractsGenerateValueImplementations() {
        assertEquals(EmptyAppearance(), EmptyAppearance())
        assertEquals(EmptyAppearance(), EmptyAppearance().copy())
        assertEquals(EmptyAppearance().hashCode(), EmptyAppearance().hashCode())
    }
}
