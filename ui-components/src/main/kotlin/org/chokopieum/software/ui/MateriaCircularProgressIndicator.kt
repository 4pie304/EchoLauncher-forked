package org.chokopieum.software.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ProgressState {
    Loading, Success, Error
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

private fun drawStick(
    drawScope: DrawScope,
    color: Color,
    strokeWidthPx: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    angleDeg: Float,
    sweepDeg: Float,
    straightness: Float,
    recoil: Float,
    offsetY: Float = 0f
) {
    val angleRad = (angleDeg * PI / 180.0).toFloat()
    
    val stickCenterX = centerX + recoil * radius * cos(angleRad)
    val stickCenterY = centerY + recoil * radius * sin(angleRad) + offsetY * radius

    if (straightness <= 0f) {
        drawScope.drawArc(
            color = color,
            startAngle = angleDeg - sweepDeg / 2,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(stickCenterX - radius, stickCenterY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    } else {
        val sweepRad = (sweepDeg * PI / 180.0).toFloat()
        
        val p1ArcX = stickCenterX + radius * cos(angleRad - sweepRad / 2)
        val p1ArcY = stickCenterY + radius * sin(angleRad - sweepRad / 2)
        
        val p2ArcX = stickCenterX + radius * cos(angleRad + sweepRad / 2)
        val p2ArcY = stickCenterY + radius * sin(angleRad + sweepRad / 2)
        
        val midArcX = stickCenterX + radius * cos(angleRad)
        val midArcY = stickCenterY + radius * sin(angleRad)
        
        val cpArcX = 2 * midArcX - (p1ArcX + p2ArcX) / 2
        val cpArcY = 2 * midArcY - (p1ArcY + p2ArcY) / 2
        
        val chordLen = 2 * radius * sin(sweepRad / 2)
        val dirX = -sin(angleRad)
        val dirY = cos(angleRad)
        
        val p1LineX = midArcX - dirX * (chordLen / 2)
        val p1LineY = midArcY - dirY * (chordLen / 2)
        
        val p2LineX = midArcX + dirX * (chordLen / 2)
        val p2LineY = midArcY + dirY * (chordLen / 2)
        
        val p1X = lerp(p1ArcX, p1LineX, straightness)
        val p1Y = lerp(p1ArcY, p1LineY, straightness)
        
        val p2X = lerp(p2ArcX, p2LineX, straightness)
        val p2Y = lerp(p2ArcY, p2LineY, straightness)
        
        val cpX = lerp(cpArcX, (p1X + p2X) / 2, straightness)
        val cpY = lerp(cpArcY, (p1Y + p2Y) / 2, straightness)
        
        val path = Path().apply {
            moveTo(p1X, p1Y)
            quadraticTo(cpX, cpY, p2X, p2Y)
        }
        
        drawScope.drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    }
}

private fun drawCheckmark(
    drawScope: DrawScope,
    color: Color,
    strokeWidthPx: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    progress: Float 
) {
    if (progress <= 0f) return

    val p1X = centerX - radius * 0.4f
    val p1Y = centerY + radius * 0.1f

    val p2X = centerX - radius * 0.1f
    val p2Y = centerY + radius * 0.4f

    val p3X = centerX + radius * 0.5f
    val p3Y = centerY - radius * 0.3f

    val path = Path()
    path.moveTo(p1X, p1Y)

    if (progress <= 0.4f) {
        val frac = progress / 0.4f
        path.lineTo(lerp(p1X, p2X, frac), lerp(p1Y, p2Y, frac))
    } else {
        path.lineTo(p2X, p2Y)
        val frac = (progress - 0.4f) / 0.6f
        path.lineTo(lerp(p2X, p3X, frac), lerp(p2Y, p3Y, frac))
    }

    drawScope.drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    )
}

private fun drawCross(
    drawScope: DrawScope,
    color: Color,
    strokeWidthPx: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    progress: Float 
) {
    if (progress <= 0f) return

    val size = radius * 0.4f

    val path = Path()

    // Первая линия крестика (сверху-слева направо-вниз)
    if (progress > 0f) {
        val p1 = progress.coerceAtMost(0.5f) / 0.5f
        path.moveTo(centerX - size, centerY - size)
        path.lineTo(
            lerp(centerX - size, centerX + size, p1),
            lerp(centerY - size, centerY + size, p1)
        )
    }
    
    // Вторая линия крестика (сверху-справа налево-вниз)
    if (progress > 0.5f) {
        val p2 = (progress - 0.5f) / 0.5f
        path.moveTo(centerX + size, centerY - size)
        path.lineTo(
            lerp(centerX + size, centerX - size, p2),
            lerp(centerY - size, centerY + size, p2)
        )
    }

    drawScope.drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    )
}

@Composable
fun MateriaCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6200EE),
    strokeWidth: Dp = 12.dp,
    state: ProgressState = ProgressState.Loading
) {
    val rotation = remember { Animatable(0f) }

    val s1CenterLocal = remember { Animatable(90f) }
    val s1Sweep = remember { Animatable(280f) }
    val s1Recoil = remember { Animatable(0f) }
    
    val s2CenterLocal = remember { Animatable(90f) }
    val s2Sweep = remember { Animatable(0f) }
    val s2Recoil = remember { Animatable(0f) }
    
    val ballPos = remember { Animatable(0f) }
    val ballScale = remember { Animatable(0f) }
    val straightness = remember { Animatable(0f) }
    
    val paddle1Y = remember { Animatable(0f) }
    val paddle2Y = remember { Animatable(0f) }
    val ballY = remember { Animatable(0f) }

    val checkmarkProgress = remember { Animatable(0f) } 
    val crossProgress = remember { Animatable(0f) }
    val stateColor = remember { Animatable(Color.Unspecified) }
    
    val arcOffsetY = remember { Animatable(0f) }

    // Инициализация начального цвета
    LaunchedEffect(color) {
        if (stateColor.value == Color.Unspecified) {
            stateColor.snapTo(color)
        } else if (state == ProgressState.Loading) {
            stateColor.animateTo(color)
        }
    }

    LaunchedEffect(state) {
        if (state == ProgressState.Loading) {
            crossProgress.snapTo(0f)
            checkmarkProgress.snapTo(0f)
            while (true) {
                rotation.snapTo(0f)
                s1Recoil.snapTo(0f)
                s2Recoil.snapTo(0f)
                paddle1Y.snapTo(0f)
                paddle2Y.snapTo(0f)
                ballY.snapTo(0f)
                checkmarkProgress.snapTo(0f)
                crossProgress.snapTo(0f)
                straightness.snapTo(0f)
                arcOffsetY.snapTo(0f)
                
                // Ускоряем вращение
                rotation.animateTo(
                    targetValue = 720f,
                    animationSpec = tween(900, easing = EaseInOutCubic)
                )
                
                launch { s1CenterLocal.animateTo(0f, tween(400, easing = EaseInOutSine)) }
                launch { s1Sweep.animateTo(60f, tween(400, easing = EaseInOutSine)) }
                launch { s2CenterLocal.animateTo(180f, tween(400, easing = EaseInOutSine)) }
                s2Sweep.animateTo(60f, tween(400, easing = EaseInOutSine))
                
                launch { straightness.animateTo(1f, tween(300, easing = EaseInOutSine)) }
                ballScale.animateTo(1f, tween(300, easing = EaseOutBack))

                // Пинг-понг ускоряем
                launch { ballY.animateTo(-0.6f, tween(250, easing = LinearEasing)) }
                launch { paddle1Y.animateTo(-0.6f, tween(250, easing = EaseInOutSine)) }
                ballPos.animateTo(1f, tween(250, easing = LinearEasing))
                launch {
                    s1Recoil.animateTo(0.15f, tween(80, easing = EaseOutQuad))
                    s1Recoil.animateTo(0f, tween(150, easing = EaseInOutQuad))
                }
                
                launch { ballY.animateTo(0.6f, tween(400, easing = LinearEasing)) }
                launch { paddle2Y.animateTo(0.6f, tween(350, easing = EaseInOutSine)) }
                ballPos.animateTo(-1f, tween(400, easing = LinearEasing))
                launch {
                    s2Recoil.animateTo(0.15f, tween(80, easing = EaseOutQuad))
                    s2Recoil.animateTo(0f, tween(150, easing = EaseInOutQuad))
                }
                
                launch { ballY.animateTo(-0.3f, tween(400, easing = LinearEasing)) }
                launch { paddle1Y.animateTo(-0.3f, tween(350, easing = EaseInOutSine)) }
                ballPos.animateTo(1f, tween(400, easing = LinearEasing))
                launch {
                    s1Recoil.animateTo(0.15f, tween(80, easing = EaseOutQuad))
                    s1Recoil.animateTo(0f, tween(150, easing = EaseInOutQuad))
                }

                launch { ballY.animateTo(0.3f, tween(400, easing = LinearEasing)) }
                launch { paddle2Y.animateTo(0.3f, tween(350, easing = EaseInOutSine)) }
                ballPos.animateTo(-1f, tween(400, easing = LinearEasing))
                launch {
                    s2Recoil.animateTo(0.15f, tween(80, easing = EaseOutQuad))
                    s2Recoil.animateTo(0f, tween(150, easing = EaseInOutQuad))
                }

                launch { ballY.animateTo(0f, tween(250, easing = LinearEasing)) }
                launch { paddle1Y.animateTo(0f, tween(250, easing = EaseInOutSine)) }
                launch { paddle2Y.animateTo(0f, tween(250, easing = EaseInOutSine)) }
                ballPos.animateTo(0f, tween(250, easing = LinearEasing))

                launch { straightness.animateTo(0f, tween(300, easing = EaseInOutSine)) }
                ballScale.animateTo(0f, tween(300, easing = EaseInOutSine))

                launch { s1CenterLocal.animateTo(90f, tween(400, easing = EaseInOutSine)) }
                launch { s1Sweep.animateTo(280f, tween(400, easing = EaseInOutSine)) }
                launch { s2CenterLocal.animateTo(90f, tween(400, easing = EaseInOutSine)) }
                s2Sweep.animateTo(0f, tween(400, easing = EaseInOutSine))
                
                delay(50)
            }
        } else {
            // ЭТАП ЗАВЕРШЕНИЯ (УСПЕХ ИЛИ ОШИБКА)
            
            // Быстрее возвращаем все назад
            launch { paddle1Y.animateTo(0f, tween(300)) }
            launch { paddle2Y.animateTo(0f, tween(300)) }
            launch { s1Recoil.animateTo(0f, tween(300)) }
            launch { s2Recoil.animateTo(0f, tween(300)) }
            
            launch { ballScale.animateTo(0f, tween(250, easing = EaseInOutSine)) }
            
            launch { straightness.animateTo(0f, tween(300, easing = EaseInOutSine)) }
            
            launch { s2Sweep.animateTo(0f, tween(300, easing = EaseInOutSine)) }
            launch { s2CenterLocal.animateTo(90f, tween(300, easing = EaseInOutSine)) }
            
            launch { s1CenterLocal.animateTo(if (state == ProgressState.Error) -90f else 90f, tween(400, easing = EaseInOutSine)) }
            
            val targetRotation = (Math.round(rotation.value / 360f) * 360f).toFloat()
            launch { rotation.animateTo(targetRotation, tween(400, easing = EaseInOutSine)) }
            
            delay(350)
            
            // Цвет плавно становится зеленым или красным
            val endColor = if (state == ProgressState.Success) Color(0xFF4CAF50) else Color(0xFFF44336)
            launch { stateColor.animateTo(endColor, tween(300)) }
            
            straightness.snapTo(0f)
            
            // Если ошибка - двигаем дугу вниз
            if (state == ProgressState.Error) {
                launch { arcOffsetY.animateTo(0.6f, tween(450, easing = EaseOutBack)) }
            }
            
            // Рисуем улыбку или грустную улыбку
            s1Sweep.animateTo(140f, tween(450, easing = EaseOutBack))
            
            delay(200)

            launch { s1Sweep.animateTo(0f, tween(250, easing = EaseInSine)) }
            delay(100)
            
            if (state == ProgressState.Success) {
                checkmarkProgress.animateTo(1f, tween(350, easing = EaseOutBack))
            } else {
                crossProgress.animateTo(1f, tween(350, easing = EaseOutBack))
            }
        }
    }

    Canvas(
        modifier = modifier.size(144.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = (size.width - strokeWidth.toPx()) / 2
        
        val currentColor = if (stateColor.value == Color.Unspecified) color else stateColor.value
        
        if (s1Sweep.value > 0f) {
            drawStick(
                drawScope = this,
                color = currentColor,
                strokeWidthPx = strokeWidth.toPx(),
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                angleDeg = rotation.value + s1CenterLocal.value,
                sweepDeg = s1Sweep.value,
                straightness = straightness.value,
                recoil = s1Recoil.value,
                offsetY = paddle1Y.value + arcOffsetY.value
            )
        }
        
        if (s2Sweep.value > 0f) {
            drawStick(
                drawScope = this,
                color = currentColor,
                strokeWidthPx = strokeWidth.toPx(),
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                angleDeg = rotation.value + s2CenterLocal.value,
                sweepDeg = s2Sweep.value,
                straightness = straightness.value,
                recoil = s2Recoil.value,
                offsetY = paddle2Y.value
            )
        }
        
        if (ballScale.value > 0f) {
            val maxDist = radius - strokeWidth.toPx()
            val currentDist = ballPos.value * maxDist
            
            val ballX = centerX + currentDist
            val ballYPx = centerY + ballY.value * radius
            
            drawCircle(
                color = currentColor,
                radius = (strokeWidth.toPx() / 2) * ballScale.value,
                center = Offset(ballX, ballYPx)
            )
        }

        if (checkmarkProgress.value > 0f) {
            drawCheckmark(
                drawScope = this,
                color = currentColor,
                strokeWidthPx = strokeWidth.toPx(),
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                progress = checkmarkProgress.value
            )
        }
        
        if (crossProgress.value > 0f) {
            drawCross(
                drawScope = this,
                color = currentColor,
                strokeWidthPx = strokeWidth.toPx(),
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                progress = crossProgress.value
            )
        }
    }
}