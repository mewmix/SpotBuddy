package com.mewmix.spotbuddy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mewmix.spotbuddy.R

private val Quicksand = FontFamily(
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_bold, FontWeight.Bold)
)

val SpotBuddyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = 58.sp,
        lineHeight = 64.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Quicksand,
        fontSize = 17.sp,
        lineHeight = 25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
)

