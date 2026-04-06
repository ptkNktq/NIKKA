plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.plugins.kotlin.multiplatform.toDep())
    compileOnly(libs.plugins.compose.multiplatform.toDep())
    compileOnly(libs.plugins.compose.compiler.toDep())
    compileOnly(libs.plugins.detekt.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

gradlePlugin {
    plugins {
        register("kotlinBase") {
            id = "nikka.kotlin.base"
            implementationClass = "NikkaKotlinBaseConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "nikka.kotlin.library"
            implementationClass = "NikkaKotlinLibraryConventionPlugin"
        }
        register("composeBase") {
            id = "nikka.compose.base"
            implementationClass = "NikkaComposeBaseConventionPlugin"
        }
        register("composeLibrary") {
            id = "nikka.compose.library"
            implementationClass = "NikkaComposeLibraryConventionPlugin"
        }
        register("composeApplication") {
            id = "nikka.compose.application"
            implementationClass = "NikkaComposeApplicationConventionPlugin"
        }
    }
}
