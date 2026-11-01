package org.msc.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalLiquidMotion = staticCompositionLocalOf { true }

@Composable internal fun LiquidBackdrop(accent: Color) {
    Canvas(Modifier.fillMaxSize().background(Color(0xff0b111a))) {
        drawRect(Brush.radialGradient(listOf(accent.copy(alpha = .22f), Color.Transparent),
            center = Offset(size.width * .1f, size.height * .12f), radius = size.width * 1.1f))
        drawRect(Brush.radialGradient(listOf(Color(0xff558da5).copy(alpha = .17f), Color.Transparent),
            center = Offset(size.width * 1.05f, size.height * .58f), radius = size.width * .95f))
        drawRect(Brush.radialGradient(listOf(accent.copy(alpha = .09f), Color.Transparent),
            center = Offset(size.width * .3f, size.height), radius = size.width * .9f))
    }
}

@Composable internal fun GlassPanel(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(modifier, shape = shape, color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .27f),
            Color.White.copy(alpha = .04f), accent.copy(alpha = .18f))))) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color.White.copy(alpha = .10f),
            Color(0xff253441).copy(alpha = .78f), Color(0xff131d2a).copy(alpha = .88f))))) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = .08f), Color.Transparent),
                    center = Offset(size.width * .2f, 0f), radius = size.width * .8f))
            }
            content()
        }
    }
}

@Composable internal fun Modifier.liquidPress(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val enabled = LocalLiquidMotion.current
    val scale by animateFloatAsState(if (pressed && enabled) .975f else 1f,
        if (enabled) spring(dampingRatio = .78f, stiffness = 550f) else snap(), label = "Glass press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable internal fun LiquidNavigation(destination: String, onSelect: (String) -> Unit) {
    val pages = listOf("Home" to "home", "Library" to "library", "Search" to "search", "Player" to "disc")
    val accent = MaterialTheme.colorScheme.primary
    val motion = LocalLiquidMotion.current
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(70.dp).selectableGroup()) {
            val slot = maxWidth / pages.size
            val target = slot * pages.indexOfFirst { it.first == destination }.coerceAtLeast(0)
            val offset by animateDpAsState(target, if (motion) spring(dampingRatio = .82f, stiffness = 440f) else snap(), label = "Liquid navigation")
            Box(Modifier.offset(x = offset).padding(5.dp).width(slot - 10.dp).height(60.dp)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = .28f), accent.copy(alpha = .10f))), RoundedCornerShape(26.dp))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .32f), Color.White.copy(alpha = .02f))), RoundedCornerShape(26.dp)))
            Row(Modifier.fillMaxSize()) {
                pages.forEach { (name, icon) ->
                    val source = remember { MutableInteractionSource() }
                    Column(Modifier.weight(1f).fillMaxHeight().liquidPress(source)
                        .selectable(destination == name, interactionSource = source, indication = null, role = Role.Tab, onClick = { onSelect(name) }),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        val color = if (name == destination) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        CompositionLocalProvider(LocalContentColor provides color) {
                            AppIcon(icon)
                            Text(name, Modifier.padding(top = 3.dp), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
