import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.file.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import kotlin.io.path.pathString
import kotlin.math.absoluteValue

object CombatInteractor {
  private var scope: CoroutineScope? = null

  var possiblePaths = mutableListOf<Path>()
  var selectedPath: String? = null
  var mostRecentEventTimestamp: Long = 0

  private val ATTACK_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.+)\\|r attacked (.+)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r and caused \\|c[0-9a-fA-F]{8}(.*)\\|r \\|c[0-9a-fA-F]{8}(.*)\\|r \\(\\|c[0-9a-fA-F]{8}(.*)\\|r\\)!")
  private val HEAL_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r targeted (.*)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r to restore \\|c[0-9a-fA-F]{8}(.*)\\|r (\\w+).")
  private val IS_CASTING: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r is casting \\|c[0-9a-fA-F]{8}(.*)\\|r")
  private val SUCCESSFUL_CAST: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r successfully cast \\|c[0-9a-fA-F]{8}(.*)\\|r")
  private val BUFF_GAINED_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r gained the buff: \\|c[0-9a-fA-F]{8}(.*)\\|r")
  private val BUFF_ENDED_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r's \\|c[0-9a-fA-F]{8}(.*)\\|r buff ended\\.")
  private val DEBUFF_GAINED: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r was struck by a \\|c[0-9a-fA-F]{8}(.*)\\|r debuff!")
  private val DEBUFF_ENDED: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.*)\\|r's \\|c[0-9a-fA-F]{8}(.*)\\|r debuff cleared")

  // Track Damage Amounts and Heals by Player

  // used to force recomposition of the UI
  private val _damageByPlayer = MutableStateFlow<MutableMap<String, Long>>(mutableMapOf())
  val damageByPlayer: StateFlow<MutableMap<String, Long>> = _damageByPlayer

  private val _healsByPlayer = MutableStateFlow<MutableMap<String, Long>>(mutableMapOf())
  val healsByPlayer: StateFlow<MutableMap<String, Long>> = _healsByPlayer

  private val _retributionByPlayer = MutableStateFlow<MutableMap<String, Long>>(mutableMapOf())
  val retributionByPlayer: StateFlow<MutableMap<String, Long>> = _retributionByPlayer

  // super tracker
  private val _targetEvents = MutableStateFlow<List<CombatEvent>>(listOf())
  val targetEvents: StateFlow<List<CombatEvent>> = _targetEvents

  private val _targetCurrentlyCasting = MutableStateFlow<String>("")
  val targetCurrentlyCasting: StateFlow<String> = _targetCurrentlyCasting

  fun resetStats() {
    _damageByPlayer.value = mutableMapOf()
    _healsByPlayer.value = mutableMapOf()
    _retributionByPlayer.value = mutableMapOf()
  }

  // called before the interaction event loop is started
  init {
    locateCombatLog()
  }

  /*
   * Recursively searches for Combat.log files in the player's Documents folder and populates a
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

  /*
   * Main interaction event loop. Watches the selected log file for changes and parses new lines.
   */
  fun start() {

    // GUARD: Cancel the previous scope and start a new one if already active
    if (scope == null || scope?.isActive == true) {
      scope?.cancel()
      scope = CoroutineScope(Dispatchers.Default)
    }

    // Launch the event loop
    scope?.launch {
      delay(1000)
      while (isActive) {
        if (selectedPath.isNullOrBlank()) continue
        val logPath = Paths.get(selectedPath!!)
        var lastIndex = Files.lines(logPath).count()
        while (isActive) {
          Files.newBufferedReader(logPath).use { reader ->
            val lines = reader.lines().skip(lastIndex).iterator()
            while (lines.hasNext()) {
              val line = lines.next()
              parseLines(listOf(line))
              lastIndex++
            }
          }
          delay(50) // delay checking to see if we have a valid file path
        }
      }
    }
    scope?.launch {
      while (isActive) {
        pruneOldEvents()
        delay(1000)
      }
    }
  }

  /*
   * Turns log lines into CombatEvents.
   */
  private suspend fun parseLines(lines: List<String>): List<CombatEvent> {
    val events = mutableListOf<CombatEvent>()

    for (line in lines) {

      // Build Attack Objects
      var matcher = ATTACK_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = AttackEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          target = matcher.group(3),
          damage = matcher.group(5).toInt().absoluteValue,
          spell = matcher.group(4),
          critical = false,
        )
        postDamage(event)
        mostRecentEventTimestamp = event.timestamp
        continue
      }

      // Build Heals Objects
      matcher = HEAL_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = HealEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          target = matcher.group(3),
          amount = matcher.group(5).toInt(),
          spell = matcher.group(4),
          critical = false,
        )
        postHeal(event)
        mostRecentEventTimestamp = event.timestamp
        continue
      }

      // Build Casting Objects
      matcher = IS_CASTING.matcher(line)
      if (matcher.find()) {
        val event = CastingEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          spell = matcher.group(3),
        )
        postCasting(event)
        mostRecentEventTimestamp = event.timestamp
        continue
      }

      // Build Successful Cast Objects
      matcher = SUCCESSFUL_CAST.matcher(line)
      if (matcher.find()) {
        val event = SuccessfulCastEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          spell = matcher.group(3),
        )
        mostRecentEventTimestamp = event.timestamp
        continue
      }

      // Build Buff Objects
      matcher = BUFF_GAINED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = BuffGainedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          buff = matcher.group(3),
        )
        processBuffGained(event)
        mostRecentEventTimestamp = event.timestamp
        continue
      }

      // Build Buff Ended Objects
      matcher = BUFF_ENDED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = BuffEndedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          buff = matcher.group(3),
        )
        processBuffEnded(event)
        mostRecentEventTimestamp = event.timestamp
        continue
      }

      // Build Struck by Debuff Objects
      matcher = DEBUFF_GAINED.matcher(line)
      if (matcher.find()) {
        val event = DebuffGainedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          debuff = matcher.group(3),
        )
        continue
      }

      // Build Debuff Ended Objects
      matcher = DEBUFF_ENDED.matcher(line)
      if (matcher.find()) {
        val event = DebuffEndedEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          target = matcher.group(2),
          debuff = matcher.group(3),
        )
        continue
      }

    }
    return events
  }

  /*
   * Generates
   */
  private fun logCombatEvent(event: CombatEvent) {
//    val currentMap = _lastCombatEventsByPlayer.value.toMutableMap()
//    val currentList = currentMap[event.caster]?.toMutableList() ?: mutableListOf()
//    currentList.add(event)
//    currentMap[event.caster] = currentList
//    _lastCombatEventsByPlayer.value = currentMap
  }

  private fun postDamage(event: AttackEvent) {
    val currentMap = _damageByPlayer.value.toMutableMap()
    val totalDamage = currentMap[event.caster]?.plus(event.damage) ?: event.damage.toLong()
    currentMap[event.caster] = totalDamage
    _damageByPlayer.value = currentMap

    // involves tracked target?
    if (event.caster == AppState.currentTargetName.value || event.target == AppState.currentTargetName.value) {
      val currentTargetEvents = _targetEvents.value.toMutableList()
      currentTargetEvents.add(event)
      _targetEvents.value = currentTargetEvents
    }
  }

  private fun postHeal(event: HealEvent) {
    val currentMap = _healsByPlayer.value.toMutableMap()
    val totalHeal = currentMap[event.caster]?.plus(event.amount) ?: event.amount.toLong()
    currentMap[event.caster] = totalHeal
    _healsByPlayer.value = currentMap
  }

  private fun postCasting(event: CastingEvent) {
    if (event.caster == AppState.currentTargetName.value) {
      _targetCurrentlyCasting.value = "" // force state update nya
      _targetCurrentlyCasting.value = event.spell
    }
  }

  //
  private fun processBuffGained(event: BuffGainedEvent) {
    when (event.buff) {
      "Retribution" -> {
        val currentMap = _retributionByPlayer.value.toMutableMap()
        currentMap[event.target] = event.timestamp
        _retributionByPlayer.value = currentMap
      }
    }

  }

  private fun processBuffEnded(event: BuffEndedEvent) {
    when (event.buff) {
      "Retribution" -> {
        val currentMap = _retributionByPlayer.value.toMutableMap()
        currentMap.remove(event.target)
        _retributionByPlayer.value = currentMap
      }
    }
  }

  /*
   * Prunes old retribution events from the map just in case we aren't nearby to witness them fall-off. Uses
   * the most recent timestamp from any event to compare against.
   */
  private fun pruneOldEvents() {
    val currentMap = _retributionByPlayer.value.toMutableMap()
    val keysToRemove = mutableListOf<String>()

    currentMap.forEach { (key, lastCastTimestamp) ->
      if (mostRecentEventTimestamp - lastCastTimestamp > 60000) {
        keysToRemove.add(key)
      }
    }

    keysToRemove.forEach { key ->
      currentMap.remove(key)
    }

    _retributionByPlayer.value = currentMap
  }

  fun stop() {
    scope?.cancel()
  }

  enum class CastDirection {
    INCOMING, OUTGOING
  }

  interface CombatEvent {
    val timestamp: Long
  }

  data class AttackEvent(
    override val timestamp: Long,
    val caster: String,
    val target: String,
    val damage: Int,
    val spell: String,
    val critical: Boolean,
  ) : CombatEvent

  data class HealEvent(
    override val timestamp: Long,
    val caster: String,
    val target: String,
    val amount: Int,
    val spell: String,
    val critical: Boolean,
  ) : CombatEvent

  data class CastingEvent(
    override val timestamp: Long,
    val caster: String,
    val spell: String,
  ) : CombatEvent

  data class SuccessfulCastEvent(
    override val timestamp: Long,
    val caster: String,
    val spell: String,
  ) : CombatEvent

  data class BuffGainedEvent(
    override val timestamp: Long,
    val target: String,
    val buff: String,
  ) : CombatEvent

  data class BuffEndedEvent(
    override val timestamp: Long,
    val target: String,
    val buff: String,
  ) : CombatEvent

  data class DebuffGainedEvent(
    override val timestamp: Long,
    val target: String,
    val debuff: String,
  ) : CombatEvent

  data class DebuffEndedEvent(
    override val timestamp: Long,
    val target: String,
    val debuff: String,
  ) : CombatEvent

}
