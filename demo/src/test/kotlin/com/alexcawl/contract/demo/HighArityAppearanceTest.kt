package com.alexcawl.contract.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class HighArityAppearanceTest {
    @Test
    fun moreThan22ArgumentsWorkWithFactoryDefaultsAndCopy() {
        val original = HighArityAppearance()
        val supplied = HighArityAppearance(measure = { first, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, last -> first - last })

        assertEquals(276, original.measure(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23))
        assertEquals(276, original.copy().measure(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23))
        assertEquals(-22, supplied.measure(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23))
        assertEquals(-22, supplied.copy().measure(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23))
    }
}
