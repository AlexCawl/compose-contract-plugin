package com.alexcawl.contract.demo.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alexcawl.contract.GenerateContract

@GenerateContract
interface CalendarDaySize {

    fun minWidth(): Dp

    fun minHeight(): Dp

    fun contentPadding(): PaddingValues

    companion object {
        val default: CalendarDaySize = CalendarDaySize(
            minWidth = { 48.dp },
            minHeight = { 48.dp },
            contentPadding = { PaddingValues(8.dp) },
        )
    }
}
