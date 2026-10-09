package com.alexcawl.contract.demo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                var count by rememberSaveable { mutableIntStateOf(0) }
                Surface(modifier = Modifier.fillMaxSize()) {
                    Counter(
                        contract = CounterContract(
                            title = stringResource(R.string.counter_title),
                            count = count,
                            onIncrement = { count++ },
                        ),
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
    }
}
