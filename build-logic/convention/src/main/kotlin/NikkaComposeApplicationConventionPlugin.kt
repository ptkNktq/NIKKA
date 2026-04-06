import org.gradle.api.Plugin
import org.gradle.api.Project

class NikkaComposeApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(NikkaComposeLibraryConventionPlugin::class.java)
    }
}
