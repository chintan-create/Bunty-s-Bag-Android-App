package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkCrimson,
    onPrimary = OnDarkCrimson,
    primaryContainer = DarkCrimsonContainer,
    onPrimaryContainer = OnDarkCrimsonContainer,
    secondary = DarkIndigo,
    onSecondary = OnDarkIndigo,
    secondaryContainer = DarkIndigoContainer,
    onSecondaryContainer = OnDarkIndigoContainer,
    tertiary = DarkSaffron,
    onTertiary = OnDarkSaffron,
    tertiaryContainer = DarkSaffronContainer,
    onTertiaryContainer = OnDarkSaffronContainer,
    background = MidnightBg,
    onBackground = WarmParchment,
    surface = MidnightSurface,
    onSurface = WarmParchment,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = WarmTaupe,
    surfaceContainerLowest = MidnightBg,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = MidnightSurfaceVariant,
    surfaceContainerHigh = ColorTokens.DarkSurfaceHigh,
    outline = DarkClayOutline,
    outlineVariant = ColorTokens.DarkOutlineVariant
)

private object ColorTokens {
    val DarkSurfaceHigh = androidx.compose.ui.graphics.Color(0xFF382E30)
    val DarkOutlineVariant = androidx.compose.ui.graphics.Color(0xFF3D3234)
}

private val LightColorScheme = lightColorScheme(
    primary = SindoorCrimson,
    onPrimary = OnSindoor,
    primaryContainer = TerracottaRose,
    onPrimaryContainer = OnTerracottaRose,
    secondary = AjrakhIndigo,
    onSecondary = OnAjrakhIndigo,
    secondaryContainer = IndigoMist,
    onSecondaryContainer = OnIndigoMist,
    tertiary = MarigoldSaffron,
    onTertiary = OnMarigold,
    tertiaryContainer = SaffronCream,
    onTertiaryContainer = OnSaffronCream,
    background = KhadiCream,
    onBackground = InkCharcoal,
    surface = IvoryLoom,
    onSurface = InkCharcoal,
    surfaceVariant = SandstoneWarm,
    onSurfaceVariant = EarthMuted,
    surfaceContainerLowest = IvoryLoom,
    surfaceContainerLow = KhadiCream,
    surfaceContainer = SandstoneWarm,
    surfaceContainerHigh = SandstoneDeep,
    outline = ClayOutline,
    outlineVariant = ClayOutlineVariant,
    error = BazaarError,
    onError = OnBazaarError
)

val BuntysBagShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BuntysBagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BuntysBagTypography,
        shapes = BuntysBagShapes,
        content = content
    )
}
