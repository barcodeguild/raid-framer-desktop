package core.database

import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KClass

/*
 * This singleton is used to define the schema of the database. And wraps all the database types so
 * that they can be reflectively referred to in the database configuration and throughout the app without listing them
 * all out in the configuration or data models floating around the codebase.
 */
object Schema {

  const val SCHEMA_VERSION = 1L

  class RFConfig() : RealmObject {
    var defaultLogPath: String = ""
    var tabbedDetectionEnabled: Boolean = false
    var overlayResizingEnabled: Boolean = true
    var colorAndTextDetectionEnabled: Boolean = false
    var searchEverywhere: Boolean = false
    var firstLaunch: Boolean = true
  }

  class RFWindowStates() : RealmObject {
    var combatState: RFWindowState? = null
    var settingsState: RFWindowState? = null
    var trackerState: RFWindowState? = null
    var aggroState: RFWindowState? = null
    var aboutState: RFWindowState? = null
    var filterState: RFWindowState? = null
  }

  class RFWindowState() : EmbeddedRealmObject {
    var type: Int = OverlayType.COMBAT.ordinal
    var lastPositionXDp: Float = 0f
    var lastPositionYDp: Float = 0f
    var lastWidthDp: Float = 0f
    var lastHeightDp: Float = 0f

    constructor(
      type: Int,
      lastPositionXDp: Float,
      lastPositionYDp: Float,
      lastWidthDp: Float,
      lastHeightDp: Float
    ) : this() {
      this.lastPositionXDp = lastPositionXDp
      this.lastPositionYDp = lastPositionYDp
      this.lastWidthDp = lastWidthDp
      this.lastHeightDp = lastHeightDp
    }
  }

  fun getRealmObjectClasses(): Set<KClass<out TypedRealmObject>> {
    return Schema::class.nestedClasses
      .filterIsInstance<KClass<out TypedRealmObject>>()
      .toSet()
  }

}

enum class OverlayType {
  COMBAT, SETTINGS, TRACKER, ABOUT, AGGRO, FILTERS
}
