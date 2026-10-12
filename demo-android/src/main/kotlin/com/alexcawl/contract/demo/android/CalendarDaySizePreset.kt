package com.alexcawl.contract.demo.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

object CalendarDaySizePreset {

    val default: CalendarDaySize
        @Composable
        get() = remember {
            CalendarDaySize(
                minWidth = { 48.dp },
                minHeight = { 48.dp },
                contentPadding = { PaddingValues(8.dp) },
            )
        }
}
