package com.strhodler.utxopocket.presentation.motion

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

private const val SharedAxisDistanceFraction = 0.12f
private const val SharedAxisYDistanceFraction = 0.14f

private const val FadeThroughInDuration = 300
private const val FadeThroughOutDuration = 180
private const val EffectsShortDuration = 180
private const val EffectsMediumDuration = 240

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private fun spatialSpring(): SpringSpec<IntOffset> =
    spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

private fun effectsFadeIn(duration: Int = EffectsMediumDuration) =
    tween<Float>(durationMillis = duration, easing = EmphasizedDecelerate)

private fun effectsFadeOut(duration: Int = EffectsShortDuration) =
    tween<Float>(durationMillis = duration, easing = EmphasizedAccelerate)

/**
 * Reduced-motion helper: true if system animations are disabled or accessibility prefers less motion.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    val animatorScale = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE
            )
        }.getOrDefault(1f)
    }
    val transitionScale = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE
            )
        }.getOrDefault(1f)
    }
    val animationsDisabled = animatorScale == 0f || transitionScale == 0f
    val accessibilityPrefersLessMotion = accessibilityManager?.isEnabled == true &&
        accessibilityManager.isTouchExplorationEnabled
    return animationsDisabled || accessibilityPrefersLessMotion
}

fun AnimatedContentTransitionScope<*>.fadeThroughIn(
    reducedMotion: Boolean
): EnterTransition =
    if (reducedMotion) {
        fadeIn(tween(durationMillis = EffectsMediumDuration, easing = LinearEasing))
    } else {
        fadeIn(tween(durationMillis = FadeThroughInDuration, easing = EmphasizedDecelerate))
    }

fun AnimatedContentTransitionScope<*>.fadeThroughOut(
    reducedMotion: Boolean
): ExitTransition =
    if (reducedMotion) {
        fadeOut(tween(durationMillis = EffectsShortDuration, easing = LinearEasing))
    } else {
        fadeOut(tween(durationMillis = FadeThroughOutDuration, easing = EmphasizedAccelerate))
    }

fun AnimatedContentTransitionScope<*>.sharedAxisXEnter(
    reducedMotion: Boolean,
    forward: Boolean
): EnterTransition =
    if (reducedMotion) {
        fadeIn(tween(durationMillis = EffectsMediumDuration, easing = LinearEasing))
    } else {
        val direction = if (forward) 1 else -1
        slideInHorizontally(
            animationSpec = spatialSpring(),
            initialOffsetX = { (it * SharedAxisDistanceFraction * direction).roundToInt() }
        ) + fadeIn(effectsFadeIn())
    }

fun AnimatedContentTransitionScope<*>.sharedAxisXExit(
    reducedMotion: Boolean,
    forward: Boolean
): ExitTransition =
    if (reducedMotion) {
        fadeOut(tween(durationMillis = EffectsShortDuration, easing = LinearEasing))
    } else {
        val direction = if (forward) -1 else 1
        slideOutHorizontally(
            animationSpec = spatialSpring(),
            targetOffsetX = { (it * SharedAxisDistanceFraction * direction).roundToInt() }
        ) + fadeOut(effectsFadeOut())
    }

fun AnimatedContentTransitionScope<*>.sharedAxisYEnter(
    reducedMotion: Boolean,
    forward: Boolean
): EnterTransition =
    if (reducedMotion) {
        fadeIn(tween(durationMillis = EffectsMediumDuration, easing = LinearEasing))
    } else {
        val direction = if (forward) 1 else -1
        slideInVertically(
            animationSpec = spatialSpring(),
            initialOffsetY = { (it * SharedAxisYDistanceFraction * direction).roundToInt() }
        ) + fadeIn(effectsFadeIn())
    }

fun AnimatedContentTransitionScope<*>.sharedAxisYExit(
    reducedMotion: Boolean,
    forward: Boolean
): ExitTransition =
    if (reducedMotion) {
        fadeOut(tween(durationMillis = EffectsShortDuration, easing = LinearEasing))
    } else {
        val direction = if (forward) -1 else 1
        slideOutVertically(
            animationSpec = spatialSpring(),
            targetOffsetY = { (it * SharedAxisYDistanceFraction * direction).roundToInt() }
        ) + fadeOut(effectsFadeOut())
    }
