package com.reoky.raidframer.core.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

//@Serializable
//enum class IPCMessageType {
//  @SerialName("PLAYER_CAST") PLAYER_CAST,
//  @SerialName("PLAYER_DAMAGE") PLAYER_DAMAGE,
//  @SerialName("PLAYER_HEAL") PLAYER_HEAL,
//  @SerialName("PLAYER_DEBUFF") PLAYER_DEBUFF,
//  @SerialName("PLAYER_BUFF") PLAYER_BUFF,
//  @SerialName("PLAYER_DEATH") PLAYER_DEATH,
//  @SerialName("PLAYER_INFO") PLAYER_INFO,
//  @SerialName("WORLD_EVENT") WORLD_EVENT,
//  @SerialName("FRAMES_UPDATE") FRAMES_UPDATE,
//  @SerialName("TARGET_UPDATE") TARGET_UPDATE, // when the user tab-targets to a new character
//  @SerialName("SELF_UPDATE") SELF_UPDATE, // sets the character name of the player automatically at char switch
//  @SerialName("SELF_FACTION") SELF_FACTION, // notifies the app of the players' faction (for enemy/friendly coloring & may change on character switch!)
//  @SerialName("DUEL_STARTED") DUEL_STARTED,
//  @SerialName("DUEL_ENDED") DUEL_ENDED,
//  @SerialName("SOUND_ALERT") SOUND_ALERT,
//  @SerialName("AOE_SPLAT") AOE_SPLAT,
//  @SerialName("TEST_PING") TEST_PING,
//  @SerialName("CONFIG_UPDATE") CONFIG_UPDATE
//}

/*
 * This is the outer envelope of the JSON message structure. IE this is what encapsulates the actual payload,
 * and the payloads themselves either come straight from the game or are standardized in lua and then sent here.
 * This is why there's an outer and an inner serializer.
 * Note to self: The Lua side of things uses second timestamps while the rest of the app uses millisecond timestamps.
 */
@Serializable
sealed class IPCMessagePayload {
  abstract val version: Int
  @Serializable(with = SecondsToMillisSerializer::class)
  abstract val timestamp: Long
  // 'type' is handled automatically by AppJson configuration

  @Serializable
  @SerialName("SELF_UPDATE")
  data class SelfUpdate(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long,
    val payload: String // character name
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("SELF_FACTION")
  data class SelfFaction(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long,
    val payload: String // faction name
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("TARGET_UPDATE")
  data class TargetUpdate(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long,
    val payload: String // character name
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("COMBAT_EVENT")
  data class CombatEvent(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long,
    @Serializable(with = CombatEventUnwrapper::class)
    val payload: CombatEventPayload
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("PLAYER_DEATH")
  data class PlayerDeath(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long,
    val payload: String // player name that died
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("PLAYER_INFO")
  data class PlayerInfo(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long,
    @Serializable(with = PlayerInfoUnwrapper::class)
    val payload: PlayerInfoPayload
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("FRAMES_UPDATE")
  data class FramesUpdate(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long,
    // Specific wrapper for list
    @Serializable(with = FramesUpdatedListUnwrapper::class)
    val payload: List<RaidFramePayload>
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("TEST_PING")
  data class TestPing(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long = System.currentTimeMillis()
  ) : IPCMessagePayload()

  @Serializable
  @SerialName("CONFIG_UPDATE")
  data class ConfigUpdate(
    override val version: Int = 1,
    @Serializable(with = SecondsToMillisSerializer::class)
    override val timestamp: Long = System.currentTimeMillis()
  ) : IPCMessagePayload()

}

/**
 * A generic serializer logic that unwraps string-escaped JSON if present,
 * or accepts the raw JSON object if not escaped.
 */
open class UnwrapStringizedJsonSerializer<T>(
  tSerializer: KSerializer<T>
) : JsonTransformingSerializer<T>(tSerializer) {

  override fun transformDeserialize(element: JsonElement): JsonElement {
    return if (element is JsonPrimitive && element.isString) {
      // It's a string (e.g., "{\"key\": \"value\"}"), parse it into a JsonElement
      AppJson.parseToJsonElement(element.content)
    } else {
      // It's already a regular JsonObject or JsonArray, pass it through
      element
    }
  }
}

// Specific implementations for your types
object PlayerInfoUnwrapper : UnwrapStringizedJsonSerializer<PlayerInfoPayload>(
  PlayerInfoPayload.serializer()
)
object CombatEventUnwrapper : UnwrapStringizedJsonSerializer<CombatEventPayload>(
  CombatEventPayload.serializer()
)

// Because it's a list of raid frames and not a single object
object FramesUpdatedListUnwrapper : UnwrapStringizedJsonSerializer<List<RaidFramePayload>>(
  ListSerializer(RaidFramePayload.serializer())
)

// this fixes the issue of lua sending timestamps in seconds while the app uses milliseconds
// at the serialization layer so any fields with the @Serializable(with = SecondsToMillisSerializer::class)
// annotation will be automatically converted (so we don't have to do it manually in each data class)
object SecondsToMillisSerializer : KSerializer<Long> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SecondsToMillis", PrimitiveKind.LONG)

  // from seconds to ms
  override fun deserialize(decoder: Decoder): Long {
    return decoder.decodeLong() * 1000
  }

  // from ms to seconds
  override fun serialize(encoder: Encoder, value: Long) {
    encoder.encodeLong(value / 1000)
  }
}