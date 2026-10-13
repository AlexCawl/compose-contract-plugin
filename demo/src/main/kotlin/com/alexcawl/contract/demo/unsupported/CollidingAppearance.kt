package com.alexcawl.contract.demo.unsupported

import com.alexcawl.contract.GenerateContract

// These examples stay commented so the catalogue compiles. Uncomment one to inspect its diagnostic.

// Each method needs its own callback name, regardless of JVM signatures.
// @GenerateContract
// interface CollidingAppearance {
//     fun color(): Int
//     fun Color(): Int // Both container getters would have the JVM name getColor.
// }
