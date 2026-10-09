package com.alexcawl.contract.demo.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Counter(contract: CounterContract, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(contract.title, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.counter_value, contract.count))
        Button(onClick = { contract.onIncrement() }) {
            Text(stringResource(R.string.increment))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CounterPreview() {
    MaterialTheme {
        Counter(
            contract = CounterContract(
                title = stringResource(R.string.counter_title),
                count = 3,
                onIncrement = {},
            ),
        )
    }
}
