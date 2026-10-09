package com.alexcawl.contract.demo.android

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CalendarWeek(modifier: Modifier = Modifier) {
    var selectedDay by rememberSaveable { mutableIntStateOf(0) }
    val weekdays = stringArrayResource(R.array.weekdays)
    val appearance = CalendarDayAppearance.default
    val weekend = CalendarDayAppearance.weekend

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            weekdays.forEachIndexed { index, weekday ->
                val selected = selectedDay == index
                val style = if (index >= 5) weekend else appearance
                val description = stringResource(R.string.calendar_day_description, weekday, index + 1)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(weekday, style = MaterialTheme.typography.labelMedium)
                    CalendarDay(
                        appearance = style,
                        isSelected = selected,
                        modifier = Modifier
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { selectedDay = index })
                            .semantics { contentDescription = description },
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = contentColorFor(style.foregroundColor(selected)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
        Text(stringResource(R.string.selected_day, weekdays[selectedDay], selectedDay + 1))
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalendarWeekPreview() {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface { CalendarWeek() }
    }
}
