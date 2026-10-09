package com.alexcawl.contract.demo.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarDayContractTest {
    @Test
    fun defaultSizeAndCopyPreserveUnchangedDimensions() {
        val original = CalendarDaySize.default
        val copied = original.copy(minWidth = 64.dp)

        assertEquals(48.dp, original.minWidth)
        assertEquals(48.dp, original.minHeight)
        assertEquals(PaddingValues(8.dp), original.contentPadding)
        assertEquals(64.dp, copied.minWidth)
        assertEquals(original.minHeight, copied.minHeight)
        assertEquals(original.contentPadding, copied.contentPadding)
        assertEquals(original, original.copy())
    }

    @Test
    fun copiedAppearanceOverridesOnlyForegroundColor() {
        val shape = RoundedCornerShape(4.dp)
        val original = CalendarDayAppearance(
            foregroundColor = { selected -> if (selected) Color.Blue else Color.Gray },
            foregroundShape = { shape },
            backgroundColor = { Color.White },
            backgroundShape = { shape },
        )
        val copied = original.copy(foregroundColor = { selected -> if (selected) Color.Red else Color.Yellow })

        for (selected in listOf(false, true)) {
            assertEquals(original.foregroundShape(selected), copied.foregroundShape(selected))
            assertEquals(original.backgroundColor(selected), copied.backgroundColor(selected))
            assertEquals(original.backgroundShape(selected), copied.backgroundShape(selected))
        }
        assertEquals(Color.Blue, original.foregroundColor(true))
        assertEquals(Color.Gray, original.foregroundColor(false))
        assertEquals(Color.Red, copied.foregroundColor(true))
        assertEquals(Color.Yellow, copied.foregroundColor(false))
    }
}
