package com.alexcawl.contract.demo.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

object CalendarDayAppearancePreset {

    val default: CalendarDayAppearance
        @Composable
        get() {
            val colors = MaterialTheme.colorScheme
            val shape = MaterialTheme.shapes.small
            return remember(colors, shape) {
                CalendarDayAppearance(
                    foregroundColor = { selected -> if (selected) colors.primary else colors.surfaceContainerLow },
                    foregroundShape = { shape },
                    backgroundColor = { colors.surface },
                    backgroundShape = { shape },
                )
            }
        }

    val weekend: CalendarDayAppearance
        @Composable
        get() {
            val appearance = default
            val colors = MaterialTheme.colorScheme
            return remember(appearance, colors) {
                appearance.copy(
                    foregroundColor = { selected -> if (selected) colors.tertiary else colors.tertiaryContainer },
                )
            }
        }
}
