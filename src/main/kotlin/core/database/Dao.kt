package core.database

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import core.helpers.use
import io.realm.kotlin.ext.isManaged
import java.io.File

object RFDao {

  private val realmConfig: RealmConfiguration by lazy { initialize() } // initializes on first access

  /*
   * Performs setup of the database for all the dao methods below to use.
   */
  private fun initialize(): RealmConfiguration {
    val schema = Schema.getRealmObjectClasses()

    // We don't have permissions to write to the program files directory, so we'll use the user's home directory
    val userHome = System.getProperty("user.home")
    val dbDirectory = "$userHome/.RaidFramer"

    // Create the directory if it doesn't exist
    val directory = File(dbDirectory)
    if (!directory.exists()) {
      directory.mkdirs()
    }

    return RealmConfiguration.Builder(schema)
      .directory(dbDirectory)
      .name("database.realm")
      .deleteRealmIfMigrationNeeded()
      .schemaVersion(Schema.SCHEMA_VERSION)
      .build()
  }

  suspend fun loadWindowStates(): Schema.RFWindowStates {
    val result = Realm.open(realmConfig).use { realm ->
      var configCopy: Schema.RFWindowStates? = null
      realm.write {
        val config = realm.query(Schema.RFWindowStates::class).first().find()
        configCopy = config?.let { realm.copyFromRealm(it) }
      }
      configCopy ?: Schema.RFWindowStates()
    }
    return result
  }

  suspend fun saveWindowStates(newWindowStates: Schema.RFWindowStates) {
    Realm.open(realmConfig).use { realm ->
      realm.query(Schema.RFWindowStates::class).first().find()?.let { result ->
        realm.write {
          findLatest(result)?.let { oldWindowStates ->
            oldWindowStates.combatState = newWindowStates.combatState
            oldWindowStates.settingsState = newWindowStates.settingsState
            oldWindowStates.trackerState = newWindowStates.trackerState
            oldWindowStates.aggroState = newWindowStates.aggroState
            oldWindowStates.aboutState = newWindowStates.aboutState
            oldWindowStates.filterState = newWindowStates.filterState
          }
        }
      } ?: run {
        realm.write {
          copyToRealm(newWindowStates)
        }
      }
    }
  }

  suspend fun loadConfig(): Schema.RFConfig {
    val result = Realm.open(realmConfig).use { realm ->
      var configCopy: Schema.RFConfig? = null
      realm.write {
        val config = realm.query(Schema.RFConfig::class).first().find()
        configCopy = config?.let { realm.copyFromRealm(it) }
      }
      configCopy ?: Schema.RFConfig()
    }
    return result
  }

  suspend fun saveConfig(config: Schema.RFConfig) {
    Realm.open(realmConfig).use { realm ->
      realm.query(Schema.RFConfig::class).first().find()?.let { result ->
        realm.write {
          findLatest(result)?.let { oldConfig ->
            oldConfig.defaultLogPath = config.defaultLogPath
            oldConfig.tabbedDetectionEnabled = config.tabbedDetectionEnabled
            oldConfig.overlayResizingEnabled = config.overlayResizingEnabled
            oldConfig.colorAndTextDetectionEnabled = config.colorAndTextDetectionEnabled
            oldConfig.firstLaunch = config.firstLaunch
            oldConfig.searchEverywhere = config.searchEverywhere
          }
        }
      } ?: run {
        realm.write {
          copyToRealm(config)
        }
      }
    }
  }

  fun tryMigrate() {
    TODO("Not yet implemented")
  }
}


