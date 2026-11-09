package app.neokeyboard.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot

/** State light inherits the parent's clip; it has no separate surface or shadow. */
data class ZeroIndication(val color: Color, val motion: ZeroMotion) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        StateLightNode(interactionSource, color, motion)
}

private class StateLightNode(
    private val source: InteractionSource,
    private val color: Color,
    private val motion: ZeroMotion,
) : Modifier.Node(), DrawModifierNode {
    private class Ripple(val press: PressInteraction.Press) {
        val radius = Animatable(0f)
        val alpha = Animatable(ZeroAlpha.Press)
        var expansion: Job? = null
    }
    private val ripples = mutableStateListOf<Ripple>()
    private val hover = Animatable(0f)
    private var hoverJob: Job? = null
    private var hovered = false
    private var focused = false

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter, is HoverInteraction.Exit,
                    is FocusInteraction.Focus, is FocusInteraction.Unfocus -> {
                        when (interaction) {
                            is HoverInteraction.Enter -> hovered = true
                            is HoverInteraction.Exit -> hovered = false
                            is FocusInteraction.Focus -> focused = true
                            is FocusInteraction.Unfocus -> focused = false
                        }
                        hoverJob?.cancel()
                        hoverJob = launch {
                            hover.animateTo(if (hovered || focused) ZeroAlpha.Hover else 0f, motion.effects())
                        }
                    }
                    is PressInteraction.Press -> {
                        val ripple = Ripple(interaction)
                        ripples += ripple
                        ripple.expansion = launch {
                            ripple.radius.animateTo(1f,
                                tween(if (motion.enabled) ZeroDuration.Ripple else ZeroDuration.ReducedFade,
                                    easing = ZeroEasing.Standard))
                        }
                    }
                    is PressInteraction.Release -> release(interaction.press)
                    is PressInteraction.Cancel -> release(interaction.press)
                }
            }
        }
    }

    private fun release(press: PressInteraction.Press) {
        val ripple = ripples.firstOrNull { it.press == press } ?: return
        coroutineScope.launch {
            ripple.alpha.animateTo(0f, motion.effectsSlow())
            // Release fades the light, but the original 600ms expansion always
            // runs to completion. Quick taps never truncate the wave geometry.
            ripple.expansion?.join()
            ripples.remove(ripple)
        }
    }

    override fun onDetach() { ripples.clear(); hovered = false; focused = false }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (hover.value > 0f) drawRect(color.copy(alpha = hover.value))
        ripples.forEach { ripple ->
            val radius = hypot(size.width, size.height) * ripple.radius.value
            if (radius > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to color.copy(alpha = ripple.alpha.value),
                        (1f - ZeroAlpha.Feather) to color.copy(alpha = ripple.alpha.value),
                        1f to color.copy(alpha = 0f),
                        center = ripple.press.pressPosition,
                        radius = radius,
                    ),
                    radius = radius,
                    center = ripple.press.pressPosition,
                )
            }
        }
    }
}

/** Use on a clipped surface so the same outline owns its light and hit area. */
@Composable
fun Modifier.zeroClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = interactionSource ?: remember { MutableInteractionSource() },
    indication = LocalIndication.current,
    enabled = enabled,
    role = role,
    onClickLabel = onClickLabel,
    onClick = onClick,
)

@Composable
fun rememberZeroIndication(): ZeroIndication {
    val color = MaterialTheme.colorScheme.onSurface
    val motion = LocalZeroMotion.current
    return remember(color, motion) { ZeroIndication(color, motion) }
}
