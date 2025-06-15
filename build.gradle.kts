import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("io.realm.kotlin") version "1.11.1"
}

group = "lol.rfcloud"
version = "1.5.4"

repositories {
  mavenCentral()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  google()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  // Note, if you develop a library, you should use compose.desktop.common.
  // compose.desktop.currentOs should be used in launcher-sourceSet
  // (in a separate module for demo project and in testMain).
  // With compose.desktop.common you will also lose @Preview functionality
  implementation(compose.desktop.currentOs)
  implementation("io.realm.kotlin:library-base:1.11.1")
  implementation("io.realm.kotlin:library-sync:1.11.1") // If using Device Sync
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0") // If using coroutines with the SDK
  implementation("net.java.dev.jna:jna-platform:5.8.0") // tabbed-out detection feature
  implementation("com.dorkbox:SystemTray:4.4") // system tray feature
  implementation("ch.qos.logback:logback-classic:1.5.18") // logging
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20") // accesses the schema reflectively
  implementation("net.sourceforge.tess4j:tess4j:5.11.0") // text detection feature
}

compose.desktop {
  application {
    mainClass = "MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Msi)
      packageName = "Raid Framer"
      packageVersion = version as String
      description = "Raid Framer Daemon"
      copyright = "© 2024 Raid Framer. All rights reserved."
      vendor = "by ~ catreo ~"
      windows {
        iconFile.set(project.file("raidframer.ico"))
        upgradeUuid = "547fdeb1-3ac5-4df9-9925-6ac9e7b18943"
        dirChooser = true
        msiPackageVersion = version as String
        menu = true
        shortcut = true
        menuGroup = "Raid Framer"
      }
    }
  }
}
