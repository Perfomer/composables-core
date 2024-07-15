package com.composables.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics

public data class DialogProperties(val dismissOnBackPress: Boolean = true, val dismissOnClickOutside: Boolean = true)

@Stable
public class DialogState(visible: Boolean = false) {
    public var visible: Boolean by mutableStateOf(visible)
}

@Stable
public class DialogScope internal constructor(state: DialogState) {
    internal var dialogState by mutableStateOf(state)
    internal val visibilityState = MutableTransitionState(false)
}

private val DialogStateSaver = run {
    mapSaver(save = {
        mapOf("visible" to it.visible)
    }, restore = {
        DialogState(it["visible"] as Boolean)
    })
}

@Composable
public fun rememberDialogState(visible: Boolean = false): DialogState {
    return rememberSaveable(saver = DialogStateSaver) { DialogState(visible) }
}

@Composable
public fun Dialog(state: DialogState = rememberDialogState(), properties: DialogProperties = DialogProperties(), content: @Composable() (DialogScope.() -> Unit)) {
    val scope = remember { DialogScope(state) }
    scope.visibilityState.targetState = state.visible

    LaunchedEffect(scope.visibilityState.currentState) {
        if (scope.visibilityState.isIdle && scope.visibilityState.currentState.not()) {
            scope.dialogState.visible = false
        }
    }

    if (scope.visibilityState.currentState || scope.visibilityState.targetState || scope.visibilityState.isIdle.not()) {
        NoScrimDialog(onDismissRequest = { state.visible = false }, properties = properties) {
            if (properties.dismissOnBackPress) {
                KeyDownHandler { event ->
                    return@KeyDownHandler when (event.key) {
                        Key.Back, Key.Escape -> {
                            scope.dialogState.visible = false
                            true
                        }

                        else -> false
                    }
                }
            }
            Box(Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { scope.dialogState.visible = false }
            }, contentAlignment = Alignment.Center) {
                scope.content()
            }
        }
    }
}

@Composable
public fun DialogScope.DialogContent(modifier: Modifier, enter: EnterTransition, exit: ExitTransition, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visibleState = visibilityState, enter = enter, exit = exit
    ) {
        Box(modifier = modifier.semantics {
            dialog()
        }.pointerInput(Unit) { detectTapGestures { } }) {
            content()
        }
    }
}

@Composable
expect internal fun NoScrimDialog(onDismissRequest: () -> Unit, properties: DialogProperties, content: @Composable () -> Unit)

@Composable
public fun DialogScope.Scrim(scrimColor: Color = Color.Black.copy(0.6f), enter: EnterTransition = fadeIn(), exit: ExitTransition = fadeOut()) {
    AnimatedVisibility(visibilityState, enter = enter, exit = exit) {
        Box(Modifier.fillMaxSize().background(scrimColor))
    }
}

@Composable
public fun DialogScope.Scrim(modifier: Modifier = Modifier, enter: EnterTransition = AppearInstantly, exit: ExitTransition = DisappearInstantly, content: @Composable BoxScope.() -> Unit = {}) {
    AnimatedVisibility(visibilityState, enter = enter, exit = exit) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)).focusable(false).then(modifier), contentAlignment = Alignment.Center, content = content)
    }
}