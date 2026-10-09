package com.alexcawl.contract.demo.android

import androidx.compose.runtime.Stable
import com.alexcawl.contract.GenerateContract

@Stable
@GenerateContract
interface CounterContract {
    val title: String
    val count: Int
    fun onIncrement()
}
