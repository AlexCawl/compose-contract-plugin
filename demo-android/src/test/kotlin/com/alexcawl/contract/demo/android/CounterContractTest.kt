package com.alexcawl.contract.demo.android

import org.junit.Assert.assertEquals
import org.junit.Test

class CounterContractTest {
    @Test
    fun factoryStoresValuesAndInvokesCallback() {
        var increments = 0
        val contract = CounterContract(
            title = "Counter",
            count = 2,
            onIncrement = { increments++ },
        )

        assertEquals("Counter", contract.title)
        assertEquals(2, contract.count)
        contract.onIncrement()
        assertEquals(1, increments)
    }

    @Test
    fun copyChangesCountAndPreservesCallback() {
        var increments = 0
        val original = CounterContract(title = "Counter", count = 2, onIncrement = { increments++ })
        val copied = original.copy(count = 3)

        assertEquals(2, original.count)
        assertEquals("Counter", copied.title)
        assertEquals(3, copied.count)
        copied.onIncrement()
        assertEquals(1, increments)
    }
}
