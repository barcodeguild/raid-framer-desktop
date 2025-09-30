import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val PACKAGE_ID = "com.reoky.raidframer"
val APP_VERSION = "2.0.0"

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.ksp)
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
      implementation(libs.dorkbox.systemTray)
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
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = PACKAGE_ID
      packageVersion = APP_VERSION
    }
  }
}


