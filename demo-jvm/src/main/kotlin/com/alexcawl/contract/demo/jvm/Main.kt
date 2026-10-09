package com.alexcawl.contract.demo.jvm

fun main() {
    val greeting = GreetingContract(name = "Kotlin")
    println(greeting.greet())

    val renamed = greeting.copy(name = "JVM")
    println("Copied name: ${renamed.name}")
    // copy preserves callbacks bound to the original contract.
    println("Copied greeting: ${renamed.greet()}")
}
