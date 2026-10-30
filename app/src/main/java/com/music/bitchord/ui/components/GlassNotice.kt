package com.music.bitchord.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.delay

/** What a glass notice is reporting — it decides the accent, the glyph and whether it waits. */
enum class NoticeTone { Success, Warning }

/**
 * A button on a glass notice. [primary] ones are drawn as the filled, accented
 * action; the rest read as plain text underneath it.
 */
class NoticeAction(
    val label: String,
    val primary: Boolean = true,
    val onClick: () -> Unit,
)

/** One message, as posted by a screen and shown by [GlassNoticeHost]. */
data class GlassNoticeRequest(
    val id: Long,
    val title: String,
    val message: String,
    val tone: NoticeTone,
    val detail: String? = null,
    val actions: List<NoticeAction> = emptyList(),
)

/**
 * The app's one-out-of-the-way messages: an action whose result cannot be read
 * back off the screen it was started from (changing the launcher icon is the
 * one that ships here), said out loud in liquid glass.
 *
 * A singleton rather than a remembered state passed down from the page, because
 * the notice has to be drawn *outside* the page: [GlassNoticeHost] sits beside
 * the nav bar, above the recorded backdrop layer, so its glass can sample the
 * page it is covering. Raised from inside that layer instead, the card would
 * record itself into the very backdrop it blurs.
 */
object GlassNotices {
    var current by mutableStateOf<GlassNoticeRequest?>(null)
        private set

    private var nextId = 0L

    fun show(
        title: String,
        message: String,
        tone: NoticeTone,
        detail: String? = null,
        actions: List<NoticeAction> = emptyList(),
    ) {
        nextId += 1
        current = GlassNoticeRequest(nextId, title, message, tone, detail, actions)
    }

    fun dismiss() {
        current = null
    }
}

/**
 * Draws whatever [GlassNotices] holds, over everything in this layer.
 *
 * Mounted once, at the app's root, as a sibling of the page content rather than
 * inside it — see [GlassNotices] for why that placement is what makes the card
 * real glass instead of a tinted rectangle.
 *
 * A success is shown for a moment and then leaves on its own: it asks nothing of
 * the reader, and a card in front of the app that needs a tap to clear is a
 * worse answer than none. A warning stays until it is acknowledged, because it
 * is asking for one — and back press, the scrim and the buttons all dismiss it.
 */
@Composable
fun GlassNoticeHost(modifier: Modifier = Modifier) {
    val request = GlassNotices.current
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()

    BackHandler(enabled = request != null) { GlassNotices.dismiss() }

    // Only the good news clears itself, and only while nothing is attached to
    // it: a card carrying a "try again" button must not vanish from under the
    // finger that was reaching for it.
    LaunchedEffect(request?.id) {
        val shown = request ?: return@LaunchedEffect
        if (shown.tone == NoticeTone.Success && shown.actions.isEmpty()) {
            delay(2_600)
            if (GlassNotices.current?.id == shown.id) GlassNotices.dismiss()
        }
    }

    AnimatedContent(
        targetState = request,
        transitionSpec = {
            if (reduceAnimation) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (
                    fadeIn(tween(220)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) +
                        slideInVertically { height -> height / 16 }
                    ) togetherWith (fadeOut(tween(150)) + scaleOut(targetScale = 0.96f))
            }
        },
        contentKey = { it?.id ?: -1L },
        label = "glassNotice",
        modifier = modifier,
    ) { notice ->
        if (notice == null) {
            Box(Modifier)
        } else {
            GlassNoticeCard(notice = notice, reduceAnimation = reduceAnimation)
        }
    }
}

@Composable
private fun GlassNoticeCard(notice: GlassNoticeRequest, reduceAnimation: Boolean) {
    val shape = remember { RoundedCornerShape(34.dp) }
    val iconAccent = when (notice.tone) {
        NoticeTone.Success -> Color(0xFF35E0AE)
        NoticeTone.Warning -> Color(0xFFFFA45C)
    }
    val actionAccent = when (notice.tone) {
        NoticeTone.Success -> Color(0xFF0E9C79)
        NoticeTone.Warning -> Color(0xFFC96A16)
    }
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    // Real glass: blur, vibrancy and the lens pass, sampling the page behind.
    // The solid fill is only for the devices that cannot do any of it.
    val glassOn = LocalLiquidGlassEnabled.current && isGlassSupported() && !reduceDynamicBlur
    // More tint than the nav bar takes: a message is meant to be read, and a
    // passage of artwork behind the card must not compete with the words.
    val glassTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)

    // A highlight that travels across the pane every few seconds. It is the part
    // that reads as a surface with a light on it, so it goes when motion is
    // unwelcome rather than being slowed down.
    val sheen = rememberInfiniteTransition(label = "noticeSheen")
    val sheenProgress by sheen.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "noticeSheenProgress",
    )
    val pulse by sheen.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "noticePulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // The page stays visible behind the message, dimmed rather than
            // blanked: this is a note over what you were doing, not a new screen.
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.52f)),
                ),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = GlassNotices::dismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp)
                .widthIn(max = 392.dp)
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (glassOn) {
                        Modifier.liquidGlass(shape = shape, surfaceTint = glassTint)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface, shape)
                    },
                )
                .drawWithContent {
                    drawContent()
                    if (!reduceAnimation) {
                        // A narrow band of light, swept diagonally: at 0 it is
                        // off the left edge, at 1 off the right.
                        val travel = size.width * 2.2f
                        val x = -size.width + travel * sheenProgress
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                                start = Offset(x, 0f),
                                end = Offset(x + size.width * 0.55f, size.height),
                            ),
                            size = Size(size.width, size.height),
                        )
                    }
                }
                .border(GLASS_EDGE_WIDTH, Color.White.copy(alpha = 0.18f), shape)
                // Swallows taps on the card so only the scrim dismisses it.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 22.dp, vertical = 26.dp)
                .semantics {
                    liveRegion = if (notice.tone == NoticeTone.Warning) {
                        LiveRegionMode.Assertive
                    } else {
                        LiveRegionMode.Polite
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                // A soft halo that breathes behind the glyph, as if the glass is
                // lit from inside. Static where motion is unwelcome.
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .graphicsLayer {
                            val scale = if (reduceAnimation) 1f else pulse
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.55f
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    iconAccent.copy(alpha = 0.42f),
                                    iconAccent.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                            ),
                            CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    iconAccent.copy(alpha = 0.95f),
                                    iconAccent.copy(alpha = 0.55f),
                                ),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.42f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (notice.tone) {
                            NoticeTone.Success -> Icons.Rounded.Check
                            NoticeTone.Warning -> Icons.Rounded.ErrorOutline
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = notice.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (!notice.detail.isNullOrBlank()) {
                // The platform's own sentence, in the monospaced voice of the
                // thing that actually refused: worth keeping, not worth the
                // weight of a paragraph.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .border(
                            width = GLASS_EDGE_WIDTH,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = notice.detail,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (notice.actions.isEmpty()) {
                GlassActionButton(
                    label = stringResource(R.string.got_it),
                    color = actionAccent,
                    onClick = GlassNotices::dismiss,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                notice.actions.forEachIndexed { index, action ->
                    if (action.primary) {
                        GlassActionButton(
                            label = action.label,
                            color = actionAccent,
                            onClick = action.onClick,
                            modifier = Modifier.padding(top = if (index == 0) 24.dp else 10.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(top = if (index == 0) 18.dp else 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = action.onClick)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (notice.actions.none { it.primary }) {
                    GlassActionButton(
                        label = stringResource(R.string.got_it),
                        color = actionAccent,
                        onClick = GlassNotices::dismiss,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
        }
    }
}

/** The one filled button on the card; it dips under a finger rather than ripple. */
@Composable
private fun GlassActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "noticeButtonPress",
    )
    val buttonShape = remember { RoundedCornerShape(19.dp) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(buttonShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(color, color.copy(alpha = 0.82f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.26f), buttonShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}
