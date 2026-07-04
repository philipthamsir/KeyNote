package com.philip.keynote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * A helper to prevent rapid double-clicks (button debouncing) in Compose.
 * Usage:
 * val safeClick = rememberSafeClick()
 * Button(onClick = { safeClick { doSomething() } }) { ... }
 */
@Composable
fun rememberSafeClick(delayMillis: Long = 1000L): (() -> Unit) -> Unit {
    var lastClickTime by remember { mutableStateOf(0L) }
    return remember(delayMillis) {
        { onClick ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > delayMillis) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}
