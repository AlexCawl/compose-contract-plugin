package com.alexcawl.contract.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class InternalAppearanceTest {
    @Test
    fun internalContractsGenerateNestedContainersAndCopy() {
        assertEquals(InternalAppearance::class.java, InternalAppearance.Contract::class.java.enclosingClass)
        assertEquals(8, InternalAppearance(inset = { 4 }).copy(inset = { 8 }).inset())
    }

    @Test
    fun copyAcceptsManualImplementationsWithExplicitContract() {
        val manual = object : InternalAppearance {
            override fun inset() = 12
            override fun contract() = InternalAppearance.Contract(inset = { 12 })
        }
        val copied = manual.copy(inset = { 24 })
        assertEquals(24, copied.inset())
    }
}
