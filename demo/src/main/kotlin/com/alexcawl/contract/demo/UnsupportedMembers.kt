package com.alexcawl.contract.demo

import com.alexcawl.contract.GenerateContract

// These examples stay commented so the catalogue compiles. Uncomment one to inspect its diagnostic.

// Each member below is independently unsupported: properties, suspend, extension, context, private, composable.
// @GenerateContract
// interface UnsupportedMembers {
//     val height: Int
//     var width: Int
//     suspend fun measure(): Int
//     fun String.decorate(): String
//     val String.width: Int
//     context(prefix: String)
//     fun contextLabel(): String
//     private fun inset(): Int = 8
//     @androidx.compose.runtime.Composable
//     fun content()
//     @get:androidx.compose.runtime.Composable
//     val label: String
// }
