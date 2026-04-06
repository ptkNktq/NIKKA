plugins {
    id("nikka.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))

            implementation(compose.materialIconsExtended)

            implementation(libs.aboutlibraries.compose.m3)
        }
    }
}
