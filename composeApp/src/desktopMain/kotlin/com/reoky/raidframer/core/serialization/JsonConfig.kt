package com.reoky.raidframer.core.serialization

import kotlinx.serialization.json.Json

val AppJson: Json = Json {
  ignoreUnknownKeys = true // helps with api version compatibility
  encodeDefaults = true // always encode default values (uses the default values from the data class list isPortal)
  isLenient = true // allows from json that isn't strictly valid (maybe a mistake we'll see)
  classDiscriminator = "type" // this is used for sealed class polymorphism it's important friends (the framework looks for type to determine which sealed class to try and inflate from json strings)
  coerceInputValues = true // coerce wrong types to the expected type when possible (eg. string "123" to int 123) in theory could fix string'd ints
}
