package com.alexcawl.contract.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class WidePaletteTest {
    @Test
    fun wideCopyPreservesCallbacksAcrossAllThreeDefaultMasks() {
        // 65 callbacks exercise three default-argument masks in the generated copy.
        val original = WidePalette(
            tone01 = { 1 },
            tone02 = { 2 },
            tone03 = { 3 },
            tone04 = { 4 },
            tone05 = { 5 },
            tone06 = { 6 },
            tone07 = { 7 },
            tone08 = { 8 },
            tone09 = { 9 },
            tone10 = { 10 },
            tone11 = { 11 },
            tone12 = { 12 },
            tone13 = { 13 },
            tone14 = { 14 },
            tone15 = { 15 },
            tone16 = { 16 },
            tone17 = { 17 },
            tone18 = { 18 },
            tone19 = { 19 },
            tone20 = { 20 },
            tone21 = { 21 },
            tone22 = { 22 },
            tone23 = { 23 },
            tone24 = { 24 },
            tone25 = { 25 },
            tone26 = { 26 },
            tone27 = { 27 },
            tone28 = { 28 },
            tone29 = { 29 },
            tone30 = { 30 },
            tone31 = { 31 },
            tone32 = { 32 },
            tone33 = { 33 },
            tone34 = { 34 },
            tone35 = { 35 },
            tone36 = { 36 },
            tone37 = { 37 },
            tone38 = { 38 },
            tone39 = { 39 },
            tone40 = { 40 },
            tone41 = { 41 },
            tone42 = { 42 },
            tone43 = { 43 },
            tone44 = { 44 },
            tone45 = { 45 },
            tone46 = { 46 },
            tone47 = { 47 },
            tone48 = { 48 },
            tone49 = { 49 },
            tone50 = { 50 },
            tone51 = { 51 },
            tone52 = { 52 },
            tone53 = { 53 },
            tone54 = { 54 },
            tone55 = { 55 },
            tone56 = { 56 },
            tone57 = { 57 },
            tone58 = { 58 },
            tone59 = { 59 },
            tone60 = { 60 },
            tone61 = { 61 },
            tone62 = { 62 },
            tone63 = { 63 },
            tone64 = { 64 },
            tone65 = { 65 },
        )
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
