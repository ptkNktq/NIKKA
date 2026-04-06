plugins {
    id("nikka.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.nikka.core.ui.resources"
}
