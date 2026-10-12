package com.alexcawl.contract.demo.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import com.alexcawl.contract.GenerateContract

@GenerateContract
interface CalendarDaySize {

    fun minWidth(): Dp

    fun minHeight(): Dp

    fun contentPadding(): PaddingValues
}
