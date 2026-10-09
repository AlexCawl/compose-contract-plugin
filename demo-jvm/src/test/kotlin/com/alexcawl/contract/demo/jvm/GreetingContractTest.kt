package com.alexcawl.contract.demo.jvm

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingContractTest {
    @Test
    fun factoryUsesDefaultMethod() {
        val greeting = GreetingContract(name = "Kotlin")

        assertEquals("Kotlin", greeting.name)
        assertEquals("Hello, Kotlin!", greeting.greet())
    }

    @Test
    fun copyChangesNameAndPreservesOriginalCallback() {
        val original = GreetingContract(name = "Kotlin")
        val copied = original.copy(name = "JVM")

        assertEquals("Kotlin", original.name)
        assertEquals("JVM", copied.name)
        assertEquals("Hello, Kotlin!", copied.greet())
    }
}
