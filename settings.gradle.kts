rootProject.name = "deck.gl-compose"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
}

include(":lib:deckgl-compose", ":demo-app")
