package com.alexcawl.contract.demo.jvm

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface JvmAppearance {
    fun width(): Int
    fun label(prefix: String = "width:"): String = prefix + width()
}
