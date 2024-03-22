import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.file.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.io.path.pathString
import kotlin.math.absoluteValue

object CombatInteractor {
  var shouldSearchEverywhere: Boolean = false
  private var scope: CoroutineScope? = null

  private var _isSearching = MutableStateFlow<Boolean>(false)
  val isSearching: StateFlow<Boolean> = _isSearching
  private var _possiblePaths = MutableStateFlow<List<Path>>(listOf())
  val possiblePaths: StateFlow<List<Path>> = _possiblePaths

  var selectedPath: String? = null
  var mostRecentEventTimestamp: Long = 0

  private val ATTACK_PATTERN: Pattern =
    Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.+)\\|r attacked (.+)\\|r using \\|c[0-9a-fA-F]{8}(.*)\\|r and caused \\|c[0-9a-fA-F]{8}(.*)\\|r \\|c[0-9a-fA-F]{8}(.*)\\|r \\(\\|c[0-9a-fA-F]{8}(.*)\\|r\\)!")
  private val ATTACK_PATTERN_NO_SKILL = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.+)\\|r attacked (.+)\\|r and caused \\|c[0-9a-fA-F]{8}(.*)\\|r \\|c[0-9a-fA-F]{8}(.*)\\|r \\(\\|c[0-9a-fA-F]{8}(.*)\\|r\\)!")
  private val ATTACK_PARRIED_PATTERN: Pattern = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(.+)\\|r attacked (.+)\\|r! Attack Parried, resulting in \\|c[0-9a-fA-F]{8}(.*)\\|r damage")

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

  /*
   * Initiating Spells : Might move this to a separate class if it gets too big.
   */
  val initiatingSpells = listOf(
    "Mana Bolts", "Divebomb", "Charge", "Tiger Strike", "Shoot Arrow", "Concussive Arrow", "Endless Arrows",
    "Absorb Lifeforce", "Enervated", "Ceaseless Fire", "Flamebolt", "Freezing Arrow", "Arc Lightning", "Electrical Arrow",
    "Rapid Strike", "Pin Down", "Blade Flurry", "Entangle", "Dancer's Touch", "Holy Bolt", "Revive", "Mana Barrier", "Fervent Healing",
    "Bull Rush", "Critical Discord"
  )

  // Track Damage Amounts and Heals by Player

  // used to force recomposition of the UI
  private val _damageByPlayer = MutableStateFlow<MutableMap<String, Long>>(mutableMapOf())
  val damageByPlayer: StateFlow<MutableMap<String, Long>> = _damageByPlayer

  private val _healsByPlayer = MutableStateFlow<MutableMap<String, Long>>(mutableMapOf())
  val healsByPlayer: StateFlow<MutableMap<String, Long>> = _healsByPlayer

  private val _retributionByPlayer = MutableStateFlow<MutableMap<String, Long>>(mutableMapOf())
  val retributionByPlayer: StateFlow<MutableMap<String, Long>> = _retributionByPlayer

  // super tracker
  private val _incomingEventsByPlayer = MutableStateFlow<MutableMap<String, List<CombatEvent>>>(mutableMapOf())
  val incomingEventsByPlayer: StateFlow<MutableMap<String, List<CombatEvent>>> = _incomingEventsByPlayer
  private val _outgoingEventsByPlayer = MutableStateFlow<MutableMap<String, List<CombatEvent>>>(mutableMapOf())
  val outgoingEventsByPlayer: StateFlow<MutableMap<String, List<CombatEvent>>> = _outgoingEventsByPlayer

  // currently casting
  private val _targetCurrentlyCasting = MutableStateFlow<String>("")
  val targetCurrentlyCasting: StateFlow<String> = _targetCurrentlyCasting

  // kept separate because eventsByPlayer is split into incoming and outgoing events and buffs are neither
  // also a time-complexity problem keeping all events in singular maps.
  private val _activeBuffsByPlayer = MutableStateFlow<MutableMap<String, List<BuffGainedEvent>>>(mutableMapOf())
  val activeBuffsByPlayer: StateFlow<MutableMap<String, List<BuffGainedEvent>>> = _activeBuffsByPlayer
  private val _activeDebuffsByPlayer = MutableStateFlow<MutableMap<String, List<DebuffGainedEvent>>>(mutableMapOf())
  val activeDebuffsByPlayer: StateFlow<MutableMap<String, List<DebuffGainedEvent>>> = _activeDebuffsByPlayer

  fun resetStats() {
    _damageByPlayer.value = mutableMapOf()
    _healsByPlayer.value = mutableMapOf()
    _retributionByPlayer.value = mutableMapOf()
    _incomingEventsByPlayer.value = mutableMapOf()
    _outgoingEventsByPlayer.value = mutableMapOf()
    _targetCurrentlyCasting.value = ""
    _activeBuffsByPlayer.value = mutableMapOf()
    _activeDebuffsByPlayer.value = mutableMapOf()
  }

  // called before the interaction event loop is started
  init {
    locateCombatLog()
  }

  /*
   * Recursively searches for Combat.log files in the player's Documents folder and populates a
   * list of possible paths for the user to choose from.
   */
  fun locateCombatLog() {

    // set active
    _isSearching.value = true

    val oneDriveDocumentsPath = Paths.get(System.getProperty("user.home"), "OneDrive", "Documents")
    val documentsPath = Paths.get(System.getProperty("user.home"), "Documents")
    val everywherePath = Paths.get(System.getProperty("user.home"))

    val searchPaths = mutableListOf<Path>()
    if (shouldSearchEverywhere) {
      if (Files.exists(everywherePath)) searchPaths.add(everywherePath)
    } else {
      if (Files.exists(oneDriveDocumentsPath)) searchPaths.add(oneDriveDocumentsPath)
      if (Files.exists(documentsPath)) searchPaths.add(documentsPath)
    }

    val possibleLogFiles = mutableListOf<Path>()

    fun seek(baseDir: Path) {
      val stream = Files.list(baseDir)
      stream.use { paths ->
        paths.forEach { path ->
          if (Files.isDirectory(path) && Files.isReadable(path)) {
            seek(path)
          } else if (path.fileName.toString().lowercase() == "combat.log" && !path.pathString.contains("LogBackups")) {
            possibleLogFiles.add(path)
          }
        }
      }
    }
    searchPaths.forEach {
      seek(it)
    }

    _possiblePaths.value = possibleLogFiles
    _isSearching.value = false
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
          delay(10)
        }
        delay(50)
      }
    }
    scope?.launch {
      while (isActive) {
        pruneOldEvents()
        pruneOldDebuffs()
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

      // Attacked for damage with a specific skill
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

      // Attacked for damage but the game didn't specify the skill
      matcher = ATTACK_PATTERN_NO_SKILL.matcher(line)
      if (matcher.find()) {
        val event = AttackEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          target = matcher.group(3),
          damage = matcher.group(4).toInt().absoluteValue,
          spell = "Auto-Attack",
          critical = false,
        )
        postDamage(event)
        mostRecentEventTimestamp = event.timestamp
        continue
      }

      // Attack Except the person parried it
      matcher = ATTACK_PARRIED_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = AttackEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          target = matcher.group(3),
          damage = matcher.group(4).toInt().absoluteValue,
          spell = "Auto-Attack",
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
        processDebuffGained(event)
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
        processDebuffEnded(event)
        continue
      }

    }
    return events
  }

  private fun postDamage(event: AttackEvent) {

    // update global counter
    val currentMap = _damageByPlayer.value.toMutableMap()
    val totalDamage = currentMap[event.caster]?.plus(event.damage) ?: event.damage.toLong()
    currentMap[event.caster] = totalDamage
    _damageByPlayer.value = currentMap

    // initiated by target?
    val currentOutgoingEventsByPlayer = _outgoingEventsByPlayer.value.toMutableMap()
    val playersCurrentOutgoingEvents = currentOutgoingEventsByPlayer[event.caster]?.toMutableList() ?: mutableListOf()
    playersCurrentOutgoingEvents.add(event)
    currentOutgoingEventsByPlayer[event.caster] = playersCurrentOutgoingEvents
    _outgoingEventsByPlayer.value = currentOutgoingEventsByPlayer

    // received by target?
    val currentIncomingEventsByPlayer = _incomingEventsByPlayer.value.toMutableMap()
    val playersCurrentIncomingEvents = currentIncomingEventsByPlayer[event.target]?.toMutableList() ?: mutableListOf()
    playersCurrentIncomingEvents.add(event)
    currentIncomingEventsByPlayer[event.target] = playersCurrentIncomingEvents
    _incomingEventsByPlayer.value = currentIncomingEventsByPlayer

    // initated damage?
    println(event.spell)
    if (AppState.config.autoTargetEnabled && AppState.config.playerName.isNotBlank()) {
      if (initiatingSpells.contains(event.spell)) {
        if (event.target != AppState.config.playerName || AppState.config.allowAutoTargetSelf) {
          AppState.currentTargetName.value = event.target
        }
      }
    }
  }

  private fun postHeal(event: HealEvent) {

    // update global heal counter
    val currentMap = _healsByPlayer.value.toMutableMap()
    val totalHeal = currentMap[event.caster]?.plus(event.amount) ?: event.amount.toLong()
    currentMap[event.caster] = totalHeal
    _healsByPlayer.value = currentMap

    // initiated by target?
    val currentOutgoingEventsByPlayer = _outgoingEventsByPlayer.value.toMutableMap()
    val playersCurrentOutgoingEvents = currentOutgoingEventsByPlayer[event.caster]?.toMutableList() ?: mutableListOf()
    playersCurrentOutgoingEvents.add(event)
    currentOutgoingEventsByPlayer[event.caster] = playersCurrentOutgoingEvents
    _outgoingEventsByPlayer.value = currentOutgoingEventsByPlayer

    // received by target?
    val currentIncomingEventsByPlayer = _incomingEventsByPlayer.value.toMutableMap()
    val playersCurrentIncomingEvents = currentIncomingEventsByPlayer[event.target]?.toMutableList() ?: mutableListOf()
    playersCurrentIncomingEvents.add(event)
    currentIncomingEventsByPlayer[event.target] = playersCurrentIncomingEvents
    _incomingEventsByPlayer.value = currentIncomingEventsByPlayer

    // initated damage?
    if (AppState.config.autoTargetEnabled && AppState.config.playerName.isNotBlank()) {
      if (initiatingSpells.contains(event.spell)) {
        if (event.target != AppState.config.playerName || AppState.config.allowAutoTargetSelf) {
          AppState.currentTargetName.value = event.target
        }
      }
    }
  }

  private fun postCasting(event: CastingEvent) {
    if (event.caster == AppState.currentTargetName.value) {
      _targetCurrentlyCasting.value = "nya" // force state update nya
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

  private fun processDebuffGained(event: DebuffGainedEvent) {
    val currentActiveDebuffsByPlayer = _activeDebuffsByPlayer.value.toMutableMap()
    val playersCurrentDebuffs = currentActiveDebuffsByPlayer[event.target]?.toMutableList() ?: mutableListOf()

    // add debuff if player doesn't already have it
    val hasDebuffAlready = playersCurrentDebuffs.any { it.debuff == event.debuff }
    if (!hasDebuffAlready) {
      playersCurrentDebuffs.add(event)
    }

    currentActiveDebuffsByPlayer[event.target] = playersCurrentDebuffs
    _activeDebuffsByPlayer.value = currentActiveDebuffsByPlayer
  }

  private fun processDebuffEnded(event: DebuffEndedEvent) {
    val currentActiveDebuffsByPlayer = _activeDebuffsByPlayer.value.toMutableMap()
    val playersCurrentDebuffs = currentActiveDebuffsByPlayer[event.target]?.toMutableList() ?: mutableListOf()

    // return a new list with all the debuffs except the one that ended
    val filteredDebuffs = playersCurrentDebuffs.filter { it.debuff != event.debuff }

    currentActiveDebuffsByPlayer[event.target] = filteredDebuffs
    _activeDebuffsByPlayer.value = currentActiveDebuffsByPlayer
  }


  /*
   * Prunes old debuff events (1 minute) just in case we are not nearby to witness them fall-off.
   */
  private fun pruneOldDebuffs() {
    val currentActiveDebuffsByPlayer = _activeDebuffsByPlayer.value.toMutableMap()
    val keysToRemove = mutableListOf<String>()

    currentActiveDebuffsByPlayer.forEach { (key, debuffs) ->
      val prunedDebuffs = debuffs.filter { mostRecentEventTimestamp - it.timestamp < 20000 }
      currentActiveDebuffsByPlayer[key] = prunedDebuffs
      if (prunedDebuffs.isEmpty()) {
        keysToRemove.add(key)
      }
    }

    _activeDebuffsByPlayer.value = currentActiveDebuffsByPlayer
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
