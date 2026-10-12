package com.alexcawl.contract.demo.android

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.alexcawl.contract.GenerateContract

@Stable
@GenerateContract
interface CalendarDayAppearance {

    fun foregroundColor(isSelected: Boolean): Color

    fun foregroundShape(isSelected: Boolean): Shape

    fun backgroundColor(isSelected: Boolean): Color

    fun backgroundShape(isSelected: Boolean): Shape
}
