package com.alexcawl.contract.demo.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun CalendarDay(
    size: CalendarDaySize = CalendarDaySize.default,
    appearance: CalendarDayAppearance = CalendarDayAppearance.default,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color = appearance.backgroundColor(isSelected), shape = appearance.backgroundShape(isSelected))
            .defaultMinSize(minWidth = size.minWidth, minHeight = size.minHeight)
            .clip(shape = appearance.foregroundShape(isSelected))
            .background(color = appearance.foregroundColor(isSelected))
            .padding(paddingValues = size.contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
