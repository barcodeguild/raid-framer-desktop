package com.reoky.raidframer.core.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TargetUpdatedPayload(
  val name: String = "",
  val type: String = "", // npc / character etc
  @SerialName("class") val classMap: Map<String, Int>, // Corresponds to the "class" key in JSON
  val factionStatus: String = "", // friendly / hostile / neutral
  val faction: String = "", // nuia / haranya / pirate / unknown
  val guild: String = ""
)
