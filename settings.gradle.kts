rootProject.name = "NIKKA"

pluginManagement {
    includeBuild("build-logic")
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

include(":detekt-rules")
include(":core:model")
include(":core:data")
include(":core:ui")
include(":feature:home")
include(":feature:settings")
include(":feature:license")
include(":composeApp")
