import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("nikka.compose.application")
    alias(libs.plugins.aboutlibraries)
}

kotlin {
    sourceSets {
        val desktopMain by getting {
            resources.srcDir("build/generated/aboutLibraries")
        }

        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
            implementation(project(":feature:home"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:license"))

            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.nikka.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NIKKA"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }
        }
    }
}

tasks.named("desktopProcessResources") {
    dependsOn("exportLibraryDefinitions")
}

detekt {
    source.setFrom(
        "src/commonMain/kotlin",
        "src/desktopMain/kotlin",
    )
}
