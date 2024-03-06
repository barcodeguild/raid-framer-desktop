package viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class CombatOverlayModel(
    var text: MutableState<String> = mutableStateOf(".: Damage Overlay :.")

)
