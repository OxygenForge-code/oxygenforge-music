package com.music.bitchord.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.music.bitchord.R
import androidx.compose.ui.window.DialogProperties

/** What a [GlassNotice] is reporting — it decides the accent and the glyph. */
enum class NoticeTone { Success, Warning }

/**
 * A liquid glass message card, for the one or two actions in Settings whose
 * outcome happens outside the app and therefore cannot be checked by looking at
 * the row afterwards — changing the launcher icon is the one that ships here.
 *
 * It is a [Dialog] rather than a card in the scrolling list for two reasons: the
 * answer must be in front of you wherever the row was scrolled to, and a failure
 * must not be able to take the app down with it. The card itself is
 * [lightweightLiquidGlass] — tint, rim and a light sweep, no backdrop capture —
 * because a dialog window has no captured backdrop of its own to sample, and
 * that keeps it cheap on the battery as well as honest about what it is drawing.
 * Without Liquid Glass (or on a device without RenderEffect support) it is the
 * same card in the theme's own surface colour; the layout, motion and wording
 * are identical either way.
 *
 * [detail] is the platform's own sentence, shown small and optional: a refusal
 * reads as a plain explanation with the technical reason underneath it.
 */
@Composable
fun GlassNotice(
    title: String,
    message: String,
    tone: NoticeTone,
    onDismiss: () -> Unit,
    detail: String? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val shape = remember { RoundedCornerShape(30.dp) }
        val accent = when (tone) {
            NoticeTone.Success -> Color(0xFF3FD8B0)
            NoticeTone.Warning -> Color(0xFFFFA45C)
        }

        // The card springs up from the scrim on the first frame it is on screen.
        // One animation drives scale, opacity, lift and the scrim together, so
        // there is no chance of them arriving a frame apart.
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { entered = true }
        val progress by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "glassNotice",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.46f * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = progress
                        val scale = 0.88f + 0.12f * progress
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - progress) * 48f
                    }
                    .lightweightLiquidGlass(
                        shape = shape,
                        fallbackColor = MaterialTheme.colorScheme.surface,
                    )
                    // A single diagonal sweep of light across the pane: the part of
                    // glass that says it has a surface and the page behind it does not.
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.05f),
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width * 0.85f, size.height),
                            ),
                        )
                    }
                    // Swallows taps on the card so only the scrim dismisses it.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.92f),
                                    accent.copy(alpha = 0.52f),
                                ),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (tone) {
                            NoticeTone.Success -> Icons.Rounded.Check
                            NoticeTone.Warning -> Icons.Rounded.ErrorOutline
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                ),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.got_it),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
