package com.alexcawl.contract.demo

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface ValueAppearance {
    fun emphasized(): Boolean
    fun byteToken(): Byte
    fun shortToken(): Short
    fun width(): Int
    fun color(): Long
    fun marker(): Char
    fun opacity(): Float
    fun scale(): Double
    fun label(): String?
    fun lineHeight(): Pixels
    fun optionalInset(): Pixels?
    fun decorations(): Map<String, List<String?>>
    fun stops(): IntArray
    fun labels(): Array<String?>
    fun transform(): (String) -> String
    fun fallback(): ValueAppearance?
}
