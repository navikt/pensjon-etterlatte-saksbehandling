/**
 * Precompiled [etterlatte.common.gradle.kts][Etterlatte_common_gradle] script plugin.
 *
 * @see Etterlatte_common_gradle
 */
class Etterlatte_commonPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Etterlatte_common_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
