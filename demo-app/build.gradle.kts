import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose)
}

kotlin {
  jvmToolchain(libs.versions.java.get().toInt())

  jvm()

  js {
    browser {
      commonWebpackConfig { outputFileName = "app.js" }
    }
    binaries.executable()
  }

  sourceSets {
    commonMain.dependencies {
      implementation(project(":lib:deckgl-compose"))
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.ui)
      implementation(libs.compose.material3)
    }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutines.swing)
    }
  }
}

compose.desktop {
  application {
    mainClass = "org.deckgl.compose.demo.MainKt"
    // deck.gl-native is called through the FFM API
    jvmArgs += "--enable-native-access=ALL-UNNAMED"
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "deck.gl-compose-demo"
      packageVersion = "1.0.0"
    }
  }
}
