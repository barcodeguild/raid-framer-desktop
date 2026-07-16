import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val APP_NAME = "Raid Framer"
val APP_VERSION = "2.2.1"
val PACKAGE_ID = "com.reoky.raidframer"

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlinSerialization)
}

kotlin {
  jvm("desktop")

  sourceSets {
    val desktopMain by getting

    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodel)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.kotlinx.serialization.json)

      implementation(libs.room.gradle.plugin)
      implementation(libs.room.runtime)
      implementation(libs.room.compiler)
      implementation(libs.sqlite.bundled)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }
    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutinesSwing)
      implementation(libs.jna)
      implementation(libs.jna.platform)
      implementation(libs.koala.core)
    }
  }
}

dependencies {
  add("kspCommonMainMetadata", libs.room.compiler)
  add("kspDesktop", libs.room.compiler)
  add("kspDesktopTest", libs.room.compiler)
}

compose.desktop {
  application {
    mainClass = "$PACKAGE_ID.MainKt"
    nativeDistributions {
      packageName = APP_NAME
      packageVersion = APP_VERSION
      description = APP_NAME
      copyright = "© 2026 Raid Framer. All rights reserved."
      vendor = "by ~ catreo ~"


      /* MSI Package Meta */
      targetFormats(TargetFormat.Msi)
      windows {
        iconFile.set(project.file("raidframer.ico"))
        upgradeUuid = "547fdeb1-3ac5-4df9-9925-6ac9e7b18943"
        dirChooser = true // whether to show the wizard ~
        msiPackageVersion = APP_VERSION
        menu = true
        shortcut = true
        menuGroup = APP_NAME

        /* Allows for combat.log files to be right-click openable in Windows */
        fileAssociation(
          mimeType = "application/x-arche-combat-log",
          extension = "log",
          description = "ArcheRage Combat Log File",
          windowsIconFile = project.file("raidframer.ico")
        )
        fileAssociation(
          mimeType = "application/x-arche-combat-log",
          extension = "rf",
          description = "Raid Framer Combat Log Slice",
          windowsIconFile = project.file("raidframer.ico")
        )
        fileAssociation(
          mimeType = "application/x-raidframer-seed-table",
          extension = "rfst",
          description = "Raid Framer Seed Table",
          windowsIconFile = project.file("raidframer.ico")
        )
      }
      jvmArgs(
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED"
      )
    }
  }
}


