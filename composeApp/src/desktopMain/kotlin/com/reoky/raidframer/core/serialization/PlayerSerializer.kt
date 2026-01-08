package com.reoky.raidframer.core.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Data structure for understanding unit info payloads. This involves switching on type to handle character (players), npcs, and mates (mounts/pets/companions)
 */
@Serializable
sealed class PlayerInfoPayload {

  @Serializable
  @SerialName("character")
  data class Character(
    val expeditionName: String = "",
    // val type: String, consumed by framework always character
    @SerialName("class") val classMap: Map<String, Int>, // Corresponds to the "class" key in JSON
    val hp: String,
    val name: String,
    val faction: String,
    @SerialName("family_name") val familyName: String,
    val level: Int,
    val heirLevel: Int,
    @SerialName("max_hp") val maxHp: String
  ) : PlayerInfoPayload()

  @Serializable
  @SerialName("npc")
  data class Npc(
    // val type: String, consumed by framework always npc
    val expeditionName: String = "",
    val isPortal: Boolean = false,
    val grade: String,
    @SerialName("class") val classMap: Map<String, Int>, // Corresponds to the "class" key in JSON
    val hp: String,
    @SerialName("portal_owner") val portalOwner: String = "",
    val kind: String,
    @SerialName("family_name") val familyName: String,
    val name: String,
    val faction: String,
    @SerialName("nick_name") val nickName: String,
    val level: Int,
    val heirLevel: Int,
    @SerialName("max_hp") val maxHp: String
  ) : PlayerInfoPayload()

  @Serializable
  @SerialName("mate")
  data class Mate(
    val expeditionName: String = "",
    // val type: String, consumed by framework always mate
    @SerialName("class") val classMap: Map<String, Int>, // Corresponds to the "class" key in JSON
    val hp: Int,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("mate_npc_name") val mateNpcName: String,
    val name: String,
    val faction: String,
    @SerialName("family_name") val familyName: String,
    val level: Int,
    val heirLevel: Int,
    @SerialName("max_hp") val maxHp: String
  ) : PlayerInfoPayload()

  // apparently farm haulers are considered "slaves" in the API
  // kind of hilarious
  @Serializable
  @SerialName("slave")
  data class Slave(
    val expeditionName: String = "",
    @SerialName("class") val classMap: Map<String, Int>, // Corresponds to the "class" key in JSON
    val hp: Int,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("family_name") val familyName: String,
    val name: String,
    val faction: String,
    val level: Int,
    val heirLevel: Int,
    @SerialName("max_hp") val maxHp: String
  ) : PlayerInfoPayload()


}
