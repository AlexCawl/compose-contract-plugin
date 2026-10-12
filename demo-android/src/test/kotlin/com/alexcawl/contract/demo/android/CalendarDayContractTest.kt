package com.alexcawl.contract.demo.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

class CalendarDayContractTest {
    @Test
    fun defaultSizeAndCopyPreserveUnchangedDimensions() = withComposition {
        val original = CalendarDaySizePreset.default
        val copied = original.copy(minWidth = { 64.dp })

        assertEquals(48.dp, original.minWidth())
        assertEquals(48.dp, original.minHeight())
        assertEquals(PaddingValues(8.dp), original.contentPadding())
        assertEquals(64.dp, copied.minWidth())
        assertEquals(original.minHeight(), copied.minHeight())
        assertEquals(original.contentPadding(), copied.contentPadding())
        assertEquals(original, original.copy())
    }

    @Test
    fun weekendPresetOverridesOnlyForegroundColor() = withComposition {
        val shape = RoundedCornerShape(4.dp)
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Color.Blue,
                surfaceContainerLow = Color.Gray,
                surface = Color.White,
                tertiary = Color.Red,
                tertiaryContainer = Color.Yellow,
            ),
            shapes = Shapes(small = shape),
        ) {
            val original = CalendarDayAppearancePreset.default
            val copied = CalendarDayAppearancePreset.weekend

            for (selected in listOf(false, true)) {
                assertEquals(shape, original.foregroundShape(selected))
                assertEquals(Color.White, original.backgroundColor(selected))
                assertEquals(shape, original.backgroundShape(selected))
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

    private fun withComposition(content: @Composable () -> Unit) {
        val recomposer = Recomposer(EmptyCoroutineContext)
        val composition = Composition(
            object : AbstractApplier<Unit>(Unit) {
                override fun insertTopDown(index: Int, instance: Unit) = Unit
                override fun insertBottomUp(index: Int, instance: Unit) = Unit
                override fun remove(index: Int, count: Int) = Unit
                override fun move(from: Int, to: Int, count: Int) = Unit
                override fun onClear() = Unit
            },
            recomposer,
        )
        try {
            composition.setContent(content)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }
}
