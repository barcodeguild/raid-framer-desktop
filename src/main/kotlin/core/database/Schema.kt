package core.database

import io.realm.kotlin.types.RealmObject
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf

/*
 * This singleton is used to define the schema of the database. And wraps all the database types so
 * that they can be reflectively referred to in the database configuration and throughout the app without listing them
 * all out in the configuration or data models floating around the codebase.
 */
object Schema {

  const val SCHEMA_VERSION = 1L

  class RFConfig() : RealmObject {
    var defaultLogPath: String = ""

    constructor(defaultLogPath: String) : this() {
      this.defaultLogPath = defaultLogPath
    }
  }

  fun getRealmObjectClasses(): Set<KClass<*>> {
    return Schema::class.declaredMemberProperties
      .mapNotNull { it.returnType.classifier as? KClass<*> }
      .filter { it.isSubclassOf(RealmObject::class) }
      .toSet()
  }

}