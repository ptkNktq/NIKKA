import org.gradle.api.Plugin
import org.gradle.api.Project

class NikkaComposeLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(NikkaComposeBaseConventionPlugin::class.java)
    }
}
