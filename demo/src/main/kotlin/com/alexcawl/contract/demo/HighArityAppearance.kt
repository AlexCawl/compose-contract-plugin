package com.alexcawl.contract.demo

import com.alexcawl.contract.GenerateContract

@GenerateContract
interface HighArityAppearance {
    fun measure(
        p001: Int,
        p002: Int,
        p003: Int,
        p004: Int,
        p005: Int,
        p006: Int,
        p007: Int,
        p008: Int,
        p009: Int,
        p010: Int,
        p011: Int,
        p012: Int,
        p013: Int,
        p014: Int,
        p015: Int,
        p016: Int,
        p017: Int,
        p018: Int,
        p019: Int,
        p020: Int,
        p021: Int,
        p022: Int,
        p023: Int,
    ): Int = p001 + p002 + p003 + p004 + p005 + p006 + p007 + p008 +
        p009 + p010 + p011 + p012 + p013 + p014 + p015 + p016 +
        p017 + p018 + p019 + p020 + p021 + p022 + p023
}
