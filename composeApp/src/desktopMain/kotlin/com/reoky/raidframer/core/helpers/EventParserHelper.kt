package com.reoky.raidframer.core.helpers

import com.reoky.raidframer.core.model.BuffEndedEvent
import com.reoky.raidframer.core.model.BuffGainedEvent
import com.reoky.raidframer.core.model.CastingEvent
import com.reoky.raidframer.core.model.CombatEvent
import com.reoky.raidframer.core.model.DamageEvent
import com.reoky.raidframer.core.model.DebuffEndedEvent
import com.reoky.raidframer.core.model.DebuffGainedEvent
import com.reoky.raidframer.core.model.HealEvent
import com.reoky.raidframer.core.model.SuccessfulCastEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.math.absoluteValue

/*
 * Used to parse lines of ArcheRage combat log into CombatEvent objects. This used to be an interactor but has
 * been converted to a helper for clarity since it has no state and is just a pure function.
 */
object EventParserHelper {

  private const val TAG = "EventParserHelper"

  private val ATTACK_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r attacked (.+)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r and caused \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r \\(\\|c[0-9a-fA-F]{8}(.*)\\|r\\|r\\)!")
  private val ATTACK_PATTERN_NO_SKILL =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r attacked (.+)\\|r and caused \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r \\|c[0-9a-fA-F]{8}(.*)\\|r \\(\\|c[0-9a-fA-F]{8}(.*)\\|r\\)!")
  private val ATTACK_PARRIED_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r attacked (.+)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r! Attack Parried, resulting in \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r damage")
  private val HEAL_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r targeted (.*)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r to restore \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r (\\w+).")
  private val IS_CASTING: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r is casting \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r")
  private val SUCCESSFUL_CAST: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r successfully cast \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r")
  private val BUFF_GAINED_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r gained the buff: \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r")
  private val BUFF_ENDED_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r's \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r buff ended\\.")
  private val DEBUFF_GAINED: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r was struck by a \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r debuff!")
  private val DEBUFF_ENDED: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\|(.+);(.+)\\|r \\|c[0-9a-fA-F]{8}(.*)\\|r\\|r debuff cleared")

  private val AR_TIMESTAMP_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  private fun parseLogTimestamp(text: String): Long {
    val ldt = LocalDateTime.parse(text, AR_TIMESTAMP_FORMATTER)
    return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
  }

  /*
   * Turns log lines into CombatEvents.
   */
  fun parseCombatEvents(lines: List<String>): List<CombatEvent> {
    val events = mutableListOf<CombatEvent>()

    for (line in lines) {

      // Attacked for damage with a specific skill
      var matcher = ATTACK_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = DamageEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = matcher.group(3),
          target = matcher.group(4),
          damage = matcher.group(6).toInt().absoluteValue,
          spell = matcher.group(5),
          critical = false,
          spellId = 43
        )
        events.add(event)
        continue
      }

      // Attacked for damage but the game didn't specify the skill
      matcher = ATTACK_PATTERN_NO_SKILL.matcher(line)
      if (matcher.find()) {
        val event = DamageEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = matcher.group(3),
          target = matcher.group(4),
          damage = matcher.group(5).toInt().absoluteValue,
          spell = "Auto-Attack",
          critical = false,
          spellId = 43
        )
        events.add(event)
        continue
      }

      // Attack Except the person parried it
      matcher = ATTACK_PARRIED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = DamageEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = matcher.group(3),
          target = matcher.group(4),
          damage = matcher.group(6).toInt().absoluteValue,
          spell = matcher.group(5),
          critical = false,
          spellId = 43
        )
        events.add(event)
        continue
      }

      // Build Heals Objects
      matcher = HEAL_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = HealEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = matcher.group(3),
          target = matcher.group(4),
          amount = matcher.group(6).toInt(),
          spell = matcher.group(5),
          critical = false,
          spellId = 43
        )
        events.add(event)
        continue
      }

      // Build Casting Objects
      matcher = IS_CASTING.matcher(line)
      if (matcher.find()) {
        val event = CastingEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = matcher.group(3),
          target = "", // No target captured in this log line
          spell = matcher.group(4),
          spellId = 43
        )
        events.add(event)
        continue
      }

      // Build Successful Cast Objects
      matcher = SUCCESSFUL_CAST.matcher(line)
      if (matcher.find()) {
        val event = SuccessfulCastEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = matcher.group(3),
          target = "", // No target captured in this log line
          spell = matcher.group(4),
          spellId = 43
        )
        events.add(event)
        continue
      }

      // Build Buff Objects
      matcher = BUFF_GAINED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = BuffGainedEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = "", // No source captured in this log line
          target = matcher.group(3),
          buff = matcher.group(4),
          buffId = 43
        )
        events.add(event)
        continue
      }

      // Build Buff Ended Objects
      matcher = BUFF_ENDED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = BuffEndedEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = "", // No source captured in this log line
          target = matcher.group(3),
          buff = matcher.group(4),
          buffId = 43
        )
        events.add(event)
        continue
      }

      // Build Struck by Debuff Objects
      matcher = DEBUFF_GAINED.matcher(line)
      if (matcher.find()) {
        val event = DebuffGainedEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = "", // No source captured in this log line
          target = matcher.group(3),
          debuff = matcher.group(4),
          debuffId = 43
        )
        events.add(event)
        continue
      }

      // Build Debuff Ended Objects
      matcher = DEBUFF_ENDED.matcher(line)
      if (matcher.find()) {
        val event = DebuffEndedEvent(
          cid = "", // CID is not captured in this log line
          timestamp = parseLogTimestamp(matcher.group(1)),
          source = "", // No source captured in this log line
          target = matcher.group(3),
          debuff = matcher.group(4),
          debuffId = 43
        )
        events.add(event)
        continue
      }

    }
    return events
  }
}
