package pl.media30.todoisto.ui.theme

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.R

// Zmienne fonty z prototypu: Manrope (tekst) + Sora (nagłówki/logo).
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variableFamily(resId: Int): FontFamily =
    if (Build.VERSION.SDK_INT >= 26) {
        FontFamily(
            listOf(400, 500, 600, 700, 800).map { w ->
                Font(resId, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
            }
        )
    } else {
        FontFamily(Font(resId))
    }

val Manrope: FontFamily by lazy { variableFamily(R.font.manrope) }
val Sora: FontFamily by lazy { variableFamily(R.font.sora) }

val Typography = Typography(
    // Sora — nagłówki ekranów ("Dzisiaj" 34/27px, tytuły widoków 26-30px)
    headlineLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.W700, fontSize = 34.sp, letterSpacing = (-0.68).sp),
    headlineMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.W700, fontSize = 30.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.W600, fontSize = 27.sp, letterSpacing = (-0.27).sp),
    titleLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.W700, fontSize = 26.sp, letterSpacing = (-0.52).sp),
    titleMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.W700, fontSize = 18.sp, letterSpacing = (-0.2).sp),
    // Manrope — treść
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.5.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.5.sp, letterSpacing = 1.26.sp)
)
