package com.reoky.raidframer.core.interactor

import kotlinx.coroutines.*

/**
 * This is a base class for all interactors in the app.
 * Each interactor is responsible for a specific piece of functionality, and will interact inside the coroutine scope.
 * Every 'tick' of the event loop the interact() method is called to perform some work in the background.
 * Classes that extend from this class will typically be object singletons, so that there are no duplicate instances.
 */
abstract class Interactor {

  // io dispatcher chosen because interact() may perform I/O operations and shouldn't update the UI directly
  // the ui layer should observe the state of the interactor and update the UI accordingly in the lol.rfcloud.main dispatcher
  val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // supervisor job to prevent child failure from cancelling the whole scope

  open fun start(delay: Long = 3000L) {
    scope.launch {
      while (isActive) {
        interact()
        delay(delay)
      }
    }
  }

  open fun stop() {
    scope.coroutineContext.cancel()
  }

  /*
   * Event loop tick is performed in a background thread. (See interactor pattern, friends!)
   * Interact() is called every 3 seconds inside the coroutine scope using the Dispatchers.IO background thread.
   */
  open suspend fun interact() {}

  fun isRunning() = scope.isActive

}