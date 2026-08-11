package com.jake.popupschool.domain.model

/**
 * Visual styling for the floating bubble and its expanded popup card.
 * Colors are stored as ARGB Long (safe against Int hex-literal overflow ambiguity)
 * and converted with `.toInt()` wherever an Android color int is required.
 */
enum class PopupStyle(
    val displayName: String,
    val backgroundColor: Long,
    val borderColor: Long,
    val headerTextColor: Long,
    val bodyTextColor: Long,
    val secondaryTextColor: Long,
    val bubbleColor: Long,
    val cornerRadiusDp: Float
) {
    PREMIUM(
        displayName = "프리미엄 (인디고 · 골드)",
        backgroundColor = 0xFFFFFFFF,
        borderColor = 0x33B4883A,
        headerTextColor = 0xFF4A3F8C,
        bodyTextColor = 0xFF1B1B1F,
        secondaryTextColor = 0xFF8A8494,
        bubbleColor = 0xFF4A3F8C,
        cornerRadiusDp = 22f
    ),
    MINIMAL(
        displayName = "미니멀 (화이트)",
        backgroundColor = 0xFFFFFFFF,
        borderColor = 0x1F000000,
        headerTextColor = 0xFF1B1B1F,
        bodyTextColor = 0xFF1B1B1F,
        secondaryTextColor = 0xFF8A8A8A,
        bubbleColor = 0xFF37474F,
        cornerRadiusDp = 12f
    ),
    DARK(
        displayName = "다크",
        backgroundColor = 0xFF1D1A29,
        borderColor = 0x33C8BEFF,
        headerTextColor = 0xFFC8BEFF,
        bodyTextColor = 0xFFEDE9F7,
        secondaryTextColor = 0xFFA79FC0,
        bubbleColor = 0xFF3E3478,
        cornerRadiusDp = 22f
    ),
    PASTEL(
        displayName = "파스텔",
        backgroundColor = 0xFFFCEFF6,
        borderColor = 0x33D98BB0,
        headerTextColor = 0xFF8B4A6B,
        bodyTextColor = 0xFF5C3A4D,
        secondaryTextColor = 0xFFB08099,
        bubbleColor = 0xFFD98BB0,
        cornerRadiusDp = 28f
    );

    companion object {
        val DEFAULT = PREMIUM
    }
}
