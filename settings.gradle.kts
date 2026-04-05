rootProject.name = "NIKKA"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":core:model")
include(":core:data")
include(":core:ui")
include(":feature:home")
include(":composeApp")
