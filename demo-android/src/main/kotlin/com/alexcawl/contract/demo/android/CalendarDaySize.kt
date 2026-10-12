package com.alexcawl.contract.demo.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import com.alexcawl.contract.GenerateContract

@Stable
@GenerateContract
interface CalendarDaySize {

    fun minWidth(): Dp

    fun minHeight(): Dp

    fun contentPadding(): PaddingValues
}
