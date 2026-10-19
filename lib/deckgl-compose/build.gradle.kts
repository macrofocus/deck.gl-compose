import org.gradle.internal.os.OperatingSystem

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose)
}

group = "org.deckgl.compose"
version = "0.1.0-SNAPSHOT"

kotlin {
  explicitApi()
  jvmToolchain(libs.versions.java.get().toInt())

  jvm()

  js {
    browser()
  }

  sourceSets {
    commonMain.dependencies {
      api(libs.compose.runtime)
      api(libs.compose.ui)
      implementation(libs.compose.foundation)
      api(libs.kotlinx.serialization.json)
      implementation(libs.kotlinx.coroutines.core)
    }
    jvmTest.dependencies {
      implementation(kotlin("test"))
      implementation(compose.desktop.currentOs)
      implementation(libs.compose.ui.test)
    }
    jsMain.dependencies {
      implementation(npm("@deck.gl/core", libs.versions.deckgl.js.get()))
      implementation(npm("@deck.gl/layers", libs.versions.deckgl.js.get()))
      implementation(npm("@deck.gl/geo-layers", libs.versions.deckgl.js.get()))
      implementation(npm("@deck.gl/aggregation-layers", libs.versions.deckgl.js.get()))
      implementation(npm("@deck.gl/json", libs.versions.deckgl.js.get()))
      // Peer dependencies of the modules above
      implementation(npm("@deck.gl/extensions", libs.versions.deckgl.js.get()))
      implementation(npm("@deck.gl/mesh-layers", libs.versions.deckgl.js.get()))
      implementation(npm("@luma.gl/core", libs.versions.lumagl.js.get()))
      implementation(npm("@luma.gl/engine", libs.versions.lumagl.js.get()))
      implementation(npm("@loaders.gl/core", libs.versions.loadersgl.js.get()))
    }
  }
}

// ---------------------------------------------------------------------------------------------
// deck.gl-native: the desktop target loads libdeckgl through the FFM API. It is built from the
// git submodule with cargo and packaged as a classpath resource for the host platform.
// ---------------------------------------------------------------------------------------------

val deckglNativeDir = rootProject.layout.projectDirectory.dir("native/deck.gl-native")

val hostPlatform: String = run {
  val os = OperatingSystem.current()
  val osName = when {
    os.isMacOsX -> "macos"
    os.isLinux -> "linux"
    os.isWindows -> "windows"
    else -> error("Unsupported OS: $os")
  }
  val arch = when (val a = System.getProperty("os.arch")) {
    "aarch64", "arm64" -> "aarch64"
    "x86_64", "amd64" -> "x86_64"
    else -> a
  }
  "$osName-$arch"
}

val nativeLibName: String = System.mapLibraryName("deckgl")

/** cargo from PATH, ~/.cargo/bin, or Homebrew's keg-only rustup. */
val cargoExecutable: Provider<String> = providers.provider {
  val home = System.getProperty("user.home")
  val candidates = System.getenv("PATH").orEmpty().split(File.pathSeparator).map { File(it, "cargo") } +
    listOf(File(home, ".cargo/bin/cargo"), File("/opt/homebrew/opt/rustup/bin/cargo"), File("/usr/local/opt/rustup/bin/cargo"))
  candidates.firstOrNull { it.canExecute() }?.absolutePath
    ?: error("cargo not found. Install Rust (https://rustup.rs) or set -Pdeckgl.skipNativeBuild=true and provide libdeckgl via -Ddeckgl.library.path")
}

val skipNativeBuild = providers.gradleProperty("deckgl.skipNativeBuild").map { it.toBoolean() }.getOrElse(false)

val cargoBuildDeckgl = tasks.register<Exec>("cargoBuildDeckgl") {
  description = "Builds deck.gl-native's C library (libdeckgl) with cargo"
  group = "build"
  val skip = skipNativeBuild
  onlyIf { !skip }
  workingDir = deckglNativeDir.asFile
  inputs.dir(deckglNativeDir.dir("crates"))
  inputs.file(deckglNativeDir.file("Cargo.toml"))
  inputs.file(deckglNativeDir.file("Cargo.lock"))
  outputs.file(deckglNativeDir.file("target/release/$nativeLibName"))
  val cargo = cargoExecutable
  doFirst {
    commandLine(cargo.get(), "build", "--release", "-p", "deck-gl-ffi")
    environment("PATH", File(cargo.get()).parent + File.pathSeparator + System.getenv("PATH"))
  }
}

val nativeResourcesDir = layout.buildDirectory.dir("generated/deckgl-native")

val packageDeckglNative = tasks.register<Sync>("packageDeckglNative") {
  description = "Packages libdeckgl as a classpath resource of the jvm target"
  dependsOn(cargoBuildDeckgl)
  val skip = skipNativeBuild
  onlyIf { !skip }
  from(deckglNativeDir.file("target/release/$nativeLibName"))
  into(nativeResourcesDir.map { it.dir("org/deckgl/compose/natives/$hostPlatform") })
}

tasks.withType<Test>().configureEach {
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}

kotlin.sourceSets.named("jvmMain") {
  resources.srcDir(files(nativeResourcesDir).builtBy(packageDeckglNative))
}
