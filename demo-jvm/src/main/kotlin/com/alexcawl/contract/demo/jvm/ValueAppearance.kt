package com.alexcawl.contract.demo.jvm

import com.alexcawl.contract.GenerateContract

@JvmInline
value class Pixels(val value: Int)

@GenerateContract
interface ValueAppearance {
    val emphasized: Boolean
    val byteToken: Byte
    val shortToken: Short
    val width: Int
    val color: Long
    val marker: Char
    val opacity: Float
    val scale: Double
    val label: String?
    val lineHeight: Pixels
    val optionalInset: Pixels?
    val decorations: Map<String, List<String?>>
    val stops: IntArray
    val labels: Array<String?>
    val transform: (String) -> String
    val fallback: ValueAppearance?
}

@GenerateContract
interface EmptyAppearance

@GenerateContract
internal interface InternalAppearance {
    val inset: Int
}
