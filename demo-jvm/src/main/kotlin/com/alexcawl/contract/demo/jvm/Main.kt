package com.alexcawl.contract.demo.jvm

fun main() {
    val values = ValueAppearance(
        emphasized = true,
        byteToken = 1,
        shortToken = 2,
        width = 48,
        color = 0xFF6750A4,
        marker = '*',
        opacity = 0.8f,
        scale = 1.0,
        label = null,
        lineHeight = Pixels(24),
        optionalInset = null,
        decorations = mapOf("weekend" to listOf("accent", null)),
        stops = intArrayOf(0, 50, 100),
        labels = arrayOf("weekday", null),
        transform = { it.uppercase() },
        fallback = null,
    )
    println("Value class: ${values.lineHeight}")
    println("Function property: ${values.transform("day")}")
    println("Value copy equality: ${values == values.copy()}")
    println("New array equality: ${values == values.copy(stops = values.stops.copyOf())}")

    val appearance = DefaultAppearance(prefix = "day:", spacing = 8, token = { "#$it" })
    println("Default method: ${appearance.label(2)}")
    println("Earlier method reference: ${appearance.references(3)}")
    println("Default parameters: ${appearance.fromParameters()}")
    println("Named argument order: ${appearance.namedOrder()}")
    println("Vararg spacing: ${appearance.spacingSum(1, 2, 3)}")
    println("Returned function: ${appearance.decorator()(4)}")

    val copied = appearance.copy(prefix = "week:", spacing = 16)
    println("Copied properties: ${copied.prefix}, ${copied.spacing}")
    // copy preserves callbacks bound to the original contract.
    println("Copied method: ${copied.label(2)}")
    println("Empty contract equality: ${EmptyAppearance() == EmptyAppearanceImpl()}")
    println("Internal contract: ${InternalAppearance(inset = 4).copy(inset = 8).inset}")
    println("65-field copy: ${WidePalette.default.copy(tone33 = 133, tone65 = 165).tone65}")
    val highArityWidth = HighArityAppearance().measure(
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
    )
    println("23-argument method: $highArityWidth")
    val slotLimitWidth = SlotLimitAppearance().measure(
        1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L,
        13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L,
        25L, 26L, 27L, 28L, 29L, 30L, 31L, 32L, 33L, 34L, 35L, 36L,
        37L, 38L, 39L, 40L, 41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L,
        49L, 50L, 51L, 52L, 53L, 54L, 55L, 56L, 57L, 58L, 59L, 60L,
        61L, 62L, 63L, 64L, 65L, 66L, 67L, 68L, 69L, 70L, 71L, 72L,
        73L, 74L, 75L, 76L, 77L, 78L, 79L, 80L, 81L, 82L, 83L, 84L,
        85L, 86L, 87L, 88L, 89L, 90L, 91L, 92L, 93L, 94L, 95L, 96L,
        97L, 98L, 99L, 100L, 101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L,
        109L, 110L, 111L, 112L, 113L, 114L, 115L, 116L, 117L, 118L, 119L, 120L,
        121L, 122L, 123L, 124L, 125L, 126L, 127L,
    )
    println("127-Long method: $slotLimitWidth")
}
