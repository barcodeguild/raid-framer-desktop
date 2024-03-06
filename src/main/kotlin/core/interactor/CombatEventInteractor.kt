import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.nio.file.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.io.path.pathString
import kotlin.math.absoluteValue

object CombatEventInteractor {
  private val scope = CoroutineScope(Dispatchers.IO)

  var possiblePaths = mutableListOf<Path>()
  var selectedPath: String? = null

  val ATTACK_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.+)\\|r attacked (.+)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r and caused \\|c[0-9a-fA-F]{8}(.*)\\|r \\|c[0-9a-fA-F]{8}(.*)\\|r \\(\\|c[0-9a-fA-F]{8}(.*)\\|r\\)!")
  val HEAL_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r targeted (.*)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r to restore \\|c[0-9a-fA-F]{8}(.*)\\|r (\\w+).")
  val IS_CASTING = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r is casting \\|c[0-9a-fA-F]{8}(.*)\\|r")
  val SUCCESSFUL_CAST = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r successfully cast \\|c[0-9a-fA-F]{8}(.*)\\|r")
  val BUFF_GAINED_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r gained the buff: \\|c[0-9a-fA-F]{8}(.*)\\|r")
  val BUFF_ENDED_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r's \\|c[0-9a-fA-F]{8}(.*)\\|r buff ended\\.")
  val DEBUFF_GAINED = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r was struck by a \\|c[0-9a-fA-F]{8}(.*)\\|r debuff!")
  val DEBUFF_ENDED = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r's \\|c[0-9a-fA-F]{8}(.*)\\|r debuff cleared")

  // Track Damage Amounts and Heals by Player
  val damageByPlayer: MutableState<Map<String, Long>> = mutableStateOf(emptyMap())
  val healsByPlayer: MutableState<Map<String, Long>> = mutableStateOf(emptyMap())

  fun postDamage(player: String, damage: Long) {
    val currentDamage = damageByPlayer.value.toMutableMap()
    val totalDamage = currentDamage[player]?.plus(damage) ?: damage
    currentDamage[player] = totalDamage
    damageByPlayer.value = currentDamage
  }

  fun postHeal(player: String, heal: Long) {
    val currentHeals = healsByPlayer.value.toMutableMap()
    val totalHeals = currentHeals[player]?.plus(heal) ?: heal
    currentHeals[player] = totalHeals
    healsByPlayer.value = currentHeals
  }

  fun resetStats() {
    damageByPlayer.value = emptyMap()
    healsByPlayer.value = emptyMap()
  }

  // called before the interactor event loop is started
  init {
    locateCombatLog()
  }
  /*
   * Recursively searches for Combat.log files in the user's Documents folder and populates a
   * list of possible paths for the user to choose from.
   */
  private fun locateCombatLog() {

    val oneDriveDocumentsPath = Paths.get(System.getProperty("user.home"), "OneDrive", "Documents")
    val documentsPath = Paths.get(System.getProperty("user.home"), "Documents")

    val baseDir = if (Files.exists(oneDriveDocumentsPath)) oneDriveDocumentsPath else documentsPath

    fun seek(baseDir: Path) {
      val stream = Files.list(baseDir)
      stream.use { paths ->
        paths.forEach { path ->
          if (Files.isDirectory(path) && Files.isReadable(path)) {
            seek(path)
          } else if (path.fileName.toString() == "Combat.log" && !path.pathString.contains("LogBackups")) {
            possiblePaths.add(path)
          }
        }
      }
    }
    seek(baseDir)
  }

  fun start() {
    if (!selectedPath.isNullOrBlank()) {
      val logPath = Paths.get(selectedPath!!)
      scope.launch {
        var lastIndex = Files.lines(logPath).count()
        while (isActive) {
          val reader = Files.newBufferedReader(logPath)
          val lines = reader.lines().skip(lastIndex).iterator()
          while (lines.hasNext()) {
            val line = lines.next()
            parseLines(listOf(line))
            lastIndex++
          }
          reader.close()
          delay(1000) // delay for a while before checking the file again
        }
      }
    }
  }

  /*
   * Turns log lines into CombatEvents.
   */
  private fun parseLines(lines: List<String>): List<CombatEvent> {
    val events = mutableListOf<CombatEvent>()

    for (line in lines) {

      // Build Attack Objects
      var matcher = ATTACK_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = AttackEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          target = matcher.group(3),
          damage = matcher.group(5).toInt().absoluteValue,
          spell = matcher.group(4),
          critical = false,
        )
        postDamage(event.caster, event.damage.toLong())
        continue
      }

      // Build Heals Objects
      matcher = HEAL_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = HealEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          target = matcher.group(3),
          amount = matcher.group(5).toInt(),
          spell = matcher.group(4),
          critical = false,
        )
        postHeal(event.caster, event.amount.toLong())
        continue
      }

      // Build Casting Objects
      matcher = IS_CASTING.matcher(line)
      if (matcher.find()) {
        val event = CastingEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          spell = matcher.group(3),
        )
        continue
      }

      // Build Successful Cast Objects
      matcher = SUCCESSFUL_CAST.matcher(line)
      if (matcher.find()) {
        val event = SuccessfulCastEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          spell = matcher.group(3),
        )
        continue
      }

      // Build Buff Objects
      matcher = BUFF_GAINED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = BuffGainedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          buff = matcher.group(3),
        )
        continue
      }

      // Build Buff Ended Objects
      matcher = BUFF_ENDED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = BuffEndedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          buff = matcher.group(3),
        )
        continue
      }

      // Build Struck by Debuff Objects
      matcher = DEBUFF_GAINED.matcher(line)
      if (matcher.find()) {
        val event = DebuffGainedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          debuff = matcher.group(3),
        )
        continue
      }

      // Build Debuff Ended Objects
      matcher = DEBUFF_ENDED.matcher(line)
      if (matcher.find()) {
        val event = DebuffEndedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          debuff = matcher.group(3),
        )
        continue
      }

    }
    return events
  }

  fun stop() {
    scope.cancel()
  }

  interface CombatEvent

  data class AttackEvent(
    val timestamp: Long,
    val caster: String,
    val target: String,
    val damage: Int,
    val spell: String,
    val critical: Boolean,
  ) : CombatEvent

  data class HealEvent(
    val timestamp: Long,
    val caster: String,
    val target: String,
    val amount: Int,
    val spell: String,
    val critical: Boolean,
  ) : CombatEvent

  data class CastingEvent(
    val timestamp: Long,
    val caster: String,
    val spell: String,
  ) : CombatEvent

  data class SuccessfulCastEvent(
    val timestamp: Long,
    val caster: String,
    val spell: String,
  ) : CombatEvent

  data class BuffGainedEvent(
    val timestamp: Long,
    val target: String,
    val buff: String,
  ) : CombatEvent

  data class BuffEndedEvent(
    val timestamp: Long,
    val target: String,
    val buff: String,
  ) : CombatEvent

  data class DebuffGainedEvent(
    val timestamp: Long,
    val target: String,
    val debuff: String,
  ) : CombatEvent

  data class DebuffEndedEvent(
    val timestamp: Long,
    val target: String,
    val debuff: String,
  ) : CombatEvent

}
