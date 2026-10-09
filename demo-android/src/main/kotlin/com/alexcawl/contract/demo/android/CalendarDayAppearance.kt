package com.alexcawl.contract.demo.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.alexcawl.contract.GenerateContract

@GenerateContract
interface CalendarDayAppearance {

    fun foregroundColor(isSelected: Boolean): Color

    fun foregroundShape(isSelected: Boolean): Shape

    fun backgroundColor(isSelected: Boolean): Color

    fun backgroundShape(isSelected: Boolean): Shape

    companion object {
        val default: CalendarDayAppearance
            @Composable get() {
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
            @Composable get() {
                val appearance = default
                val colors = MaterialTheme.colorScheme
                return remember(appearance, colors) {
                    appearance.copy(
                        foregroundColor = { selected -> if (selected) colors.tertiary else colors.tertiaryContainer },
                    )
                }
            }
    }
}
