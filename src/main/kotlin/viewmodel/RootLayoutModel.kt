package viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class RootLayoutModel(
    var text: MutableState<String> = mutableStateOf("Hello, World!")
)
