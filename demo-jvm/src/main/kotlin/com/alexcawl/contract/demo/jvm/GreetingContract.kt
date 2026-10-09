package com.alexcawl.contract.demo.jvm

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface GreetingContract {
    val name: String
    fun greet(): String = "Hello, $name!"
}
