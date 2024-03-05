import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.*
import ui.FileSelectionDialog
import java.nio.file.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.io.path.pathString

object CombatEventInteractor {
  private val scope = CoroutineScope(Dispatchers.IO)

  var possiblePaths = mutableListOf<Path>()
  var selectedPath: String? = null

  val ATTACK_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)\\|r attacked (\\w+)\\|r using \\|c[0-9a-fA-F]{8}(.*?)\\|r and caused \\|c[0-9a-fA-F]{8}(.*?)\\|r \\|c[0-9a-fA-F]{8}(.*?)\\|r")
  val HEAL_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r targeted (\\w+)|r using |c[0-9a-fA-F]{8}(.?)|r to restore |c[0-9a-fA-F]{8}(.?)|r (\\w+).")
  val IS_CASTING = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r is casting |c[0-9a-fA-F]{8}(.*?)|r")
  val SUCCESSFUL_CAST = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)\\|r successfully cast \\|c[0-9a-fA-F]{8}(.*?)\\|r")
  val BUFF_GAINED_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r gained the buff: |c[0-9a-fA-F]{8}(.*?)|r")
  val BUFF_ENDED_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r's |c[0-9a-fA-F]{8}(.*?)|r buff ended.")
  val DEBUFF_GAINED = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r was struck by a |c[0-9a-fA-F]{8}(.*?)|r debuff!")


  val DEBUFF_CLEAR_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r's (.*)( debuff cleared)")
  val TARGET_PATTERN = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r targeted (.*) using (.*)( to restore (.*))?")

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
          } else if (path.fileName.toString() == "Combat.log") {
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
        var lastIndex = Files.lines(logPath).count().toInt()
        while (isActive) {
          val reader = Files.newBufferedReader(logPath)
          val lines = reader.lines().skip(lastIndex.toLong()).iterator()
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

  private fun parseLines(lines: List<String>): List<CombatEvent> {
    val events = mutableListOf<CombatEvent>()

    for (line in lines) {
      var matcher = ATTACK_PATTERN.matcher(line)
      if (matcher.find()) {
        val event = CombatEvent(
          timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli(),
          caster = matcher.group(2),
          target = matcher.group(3),
          damage = matcher.group(5).toInt(),
          state = CastState.STRUCK,
          critical = false,
          spell = matcher.group(4)
        )
        println(event)
        continue
      }
      matcher = BUFF_END_PATTERN.matcher(line)
      if (matcher.find()) {
        // handle buff end event
        continue
      }
      matcher = DEBUFF_CLEAR_PATTERN.matcher(line)
      if (matcher.find()) {
        // handle debuff clear event
        continue
      }
      matcher = TARGET_PATTERN.matcher(line)
      if (matcher.find()) {
        // handle target event
        continue
      }
    }

    return events
  }

//  private fun parseLines(lines: List<String>): List<CombatEvent> {
//    val events = mutableListOf<CombatEvent>()
//
//    val pattern = Pattern.compile("<(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(\\w+)|r(.*)")
//    for (line in lines) {
//      val matcher = pattern.matcher(line)
//      if (matcher.find()) {
//        if (matcher.groupCount() != 3) continue
//        val timestamp = LocalDateTime.parse(matcher.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
//        val caster = matcher.group(2)
//        val eventDetail = matcher.group(3)
//
//        val event = when {
//          eventDetail.contains("successfully cast") -> {
//            val spell = eventDetail.substringAfter("successfully cast ").substringBefore("|r")
//            CombatEvent(timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(), caster, "", 0, CastState.CASTED, false, spell)
//          }
//          eventDetail.contains("attacked") -> {
//            val target = eventDetail.substringAfter("attacked ").substringBefore("|r using")
//            val spell = eventDetail.substringAfter("using ").substringBefore(" and caused")
//            val damage = eventDetail.substringAfter("caused ").substringBefore(" |cffff0000Health").replace("|cffff0000-", "").toInt()
//            val critical = eventDetail.contains("Critical damage")
//            CombatEvent(timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(), caster, target, damage, CastState.STRUCK, critical, spell)
//          }
//          eventDetail.contains("is casting") -> {
//            val spell = eventDetail.substringAfter("is casting ").substringBefore("|r")
//            CombatEvent(timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(), caster, "", 0, CastState.CASTING, false, spell)
//          }
//          eventDetail.contains("to restore") -> {
//            val target = eventDetail.substringAfter("targeted ").substringBefore("|r using")
//            val spell = eventDetail.substringAfter("using ").substringBefore(" to restore")
//            val spellEffect = eventDetail.substringAfter("to restore ").substringBefore(" health").replace("|cff00ff00", "").toInt()
//            CombatEvent(timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(), caster, target, 0, CastState.HEALED, false, spell, spellEffect)
//          }
//          else -> null
//        }
//
//        if (event != null) {
//          events.add(event)
//        }
//      }
//    }
//
//    events.forEach { println(it) }
//
//    return events
//  }

  fun stop() {
    scope.cancel()
  }

  data class CombatEvent(
    val timestamp: Long,
    val caster: String,
    val target: String,
    val damage: Int,
    val state: CastState,
    val critical: Boolean,
    val spell: String,
    val spellEffect: Int? = null
  )

  enum class CastState {
    CASTING, CASTED, STRUCK, MISSED, HEALED
  }

}