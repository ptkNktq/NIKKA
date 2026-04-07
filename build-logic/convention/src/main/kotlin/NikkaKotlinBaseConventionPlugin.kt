import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class NikkaKotlinBaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("io.gitlab.arturbosch.detekt")

        extensions.configure<KotlinMultiplatformExtension> {
            jvm("desktop")
        }

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom("$rootDir/detekt.yml")
            source.setFrom("src/commonMain/kotlin")
        }

        dependencies {
            add("detektPlugins", libs.findLibrary("detekt-formatting").get())
            add("detektPlugins", project(":detekt-rules"))
        }
    }
}

private val Project.libs
    get() = extensions.getByType(
        org.gradle.api.artifacts.VersionCatalogsExtension::class.java
    ).named("libs")
