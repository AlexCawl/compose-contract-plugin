package com.alexcawl.contract.demo.jvm

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface JvmAppearance {
    val width: Int
    fun label(prefix: String = "width:"): String = prefix + width
}
