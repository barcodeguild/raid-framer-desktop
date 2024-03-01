import kotlinx.coroutines.*
import java.nio.file.*

class CombatEventInteractor(private val filePath: Path, private val onNewLines: (List<String>) -> Unit) {
  private var lastLineCount = 0
  private val scope = CoroutineScope(Dispatchers.IO)

  private var possiblePaths = mutableListOf<Path>()

  // called before the interactor event loop is started
  init {
    seek()
  }

  /*
   * Recursively search for a file named Combat.log in the user's Documents folder and populates a
   * list of possible paths to the file for the user to choose from.
   */
  fun seek(basedir: Path = Paths.get(System.getProperty("user.home"), "Documents")) {
    val stream = Files.list(basedir)
    stream.use { paths ->
      paths.forEach { path ->
        if (Files.isDirectory(path)) {
          seek(path)
        } else if (path.fileName.toString() == "Combat.log") {
          possiblePaths.add(path)
        }
      }
    }
  }

  fun start() {
    scope.launch {
      while (isActive) {
        val lines = Files.readAllLines(filePath)
        if (lines.size > lastLineCount) {
          onNewLines(lines.subList(lastLineCount, lines.size))
          lastLineCount = lines.size
        }
        delay(1000) // delay for a while before checking the file again
      }
    }
  }

  fun stop() {
    scope.cancel()
  }
}