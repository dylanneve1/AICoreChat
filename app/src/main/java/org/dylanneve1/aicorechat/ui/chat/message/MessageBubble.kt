package org.dylanneve1.aicorechat.ui.chat.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun MessageBubble(
    isFromUser: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    isNewMessage: Boolean = false,
    content: @Composable () -> Unit,
) {
    var isVisible by remember { mutableStateOf(!isNewMessage) }

    val scaleAnim = remember { Animatable(if (isNewMessage) 0.8f else 1f) }
    val alphaAnim = remember { Animatable(if (isNewMessage) 0f else 1f) }

    LaunchedEffect(isNewMessage) {
        if (isNewMessage) {
            isVisible = true
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 100
                )
            )
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = if (isNewMessage) {
            slideInHorizontally(
                initialOffsetX = { if (isFromUser) it else -it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        } else {
            fadeIn() + scaleIn()
        },
        exit = slideOutHorizontally(
            targetOffsetX = { if (isFromUser) it else -it }
        ) + fadeOut()
    ) {
        val bubbleColor = if (isFromUser) {
            Brush.linearGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.9f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.95f)
                ),
                start = Offset(Float.POSITIVE_INFINITY, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY)
            )
        }

        val cornerRadius = 20.dp
        val tailSize = 12.dp

        Box(
            modifier = modifier
                .drawBehind {
                    drawMessageBubble(
                        isFromUser = isFromUser,
                        brush = bubbleColor,
                        cornerRadius = cornerRadius.toPx(),
                        tailSize = tailSize.toPx(),
                        alpha = alphaAnim.value
                    )
                }
                .padding(
                    start = if (isFromUser) 0.dp else tailSize,
                    end = if (isFromUser) tailSize else 0.dp,
                    top = 8.dp,
                    bottom = tailSize + 4.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.Transparent)
            ) {
                content()
            }
        }
    }
}

private fun DrawScope.drawMessageBubble(
    isFromUser: Boolean,
    brush: Brush,
    cornerRadius: Float,
    tailSize: Float,
    alpha: Float = 1f
) {
    val path = Path()
    val width = size.width
    val height = size.height
    val bodyHeight = height - tailSize

    path.addRoundRect(
        roundRect = androidx.compose.ui.geometry.RoundRect(
            left = 0f,
            top = 0f,
            right = width,
            bottom = bodyHeight,
            cornerRadius = CornerRadius(cornerRadius)
        )
    )

    val tailPath = Path()
    if (isFromUser) {
        val tailStartX = width - cornerRadius * 1.5f
        val tailBottomY = height

        tailPath.moveTo(tailStartX, bodyHeight)
        tailPath.lineTo(width - tailSize, tailBottomY)
        tailPath.lineTo(width - cornerRadius, bodyHeight)
        tailPath.close()
    } else {
        val tailStartX = cornerRadius * 1.5f

        tailPath.moveTo(tailStartX, bodyHeight)
        tailPath.lineTo(tailSize, height)
        tailPath.lineTo(cornerRadius, bodyHeight)
        tailPath.close()
    }

    path.addPath(tailPath)

    drawPath(
        path = path,
        brush = brush,
        alpha = alpha
    )
} 