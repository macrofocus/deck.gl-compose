package org.deckgl.compose.demo

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "deck.gl Compose demo",
    state = rememberWindowState(width = 1200.dp, height = 800.dp),
  ) {
    DemoApp()
  }
}
