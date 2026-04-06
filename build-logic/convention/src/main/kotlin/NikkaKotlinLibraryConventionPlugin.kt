import org.gradle.api.Plugin
import org.gradle.api.Project

class NikkaKotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(NikkaKotlinBaseConventionPlugin::class.java)
    }
}
