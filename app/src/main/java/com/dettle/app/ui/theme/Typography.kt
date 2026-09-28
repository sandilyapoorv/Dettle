package com.dettle.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dettle.app.R

val MaliFont = FontFamily(
    Font(R.font.mali_medium, FontWeight.Normal),
    Font(R.font.mali_semibold, FontWeight.Medium),
    Font(R.font.mali_bold, FontWeight.SemiBold),
    Font(R.font.mali_bold, FontWeight.Bold)
)

val PatrickHand = MaliFont
val DettleFont = MaliFont

val DettleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PatrickHand,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.sp
    )
)
