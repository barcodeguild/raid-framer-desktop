package core.interactor

import kotlinx.coroutines.*

abstract class Interactor {

  // io dispatcher chosen because interact() may perform I/O operations and shouldn't update the UI directly
  // the ui layer should observe the state of the interactor and update the UI accordingly in the main dispatcher
  private val scope = CoroutineScope(Dispatchers.IO)

  fun start() {
    scope.launch {
      delay(3000) // stagger the start of interact() to give the UI time to load
      while (isActive) {
        interact()
      }
    }
  }

  fun stop() {
    scope.coroutineContext.cancel()
  }

  /*
   * Event loop tick is performed in a background thread every 1s. (See interactor pattern, friends!)
   * Interactors in this app are used to process data. Overlays are used to present said data.
   */
  open suspend fun interact() {}

}