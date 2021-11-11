/**
 * Precompiled [etterlatte.rapids-and-rivers.gradle.kts][Etterlatte_rapids_and_rivers_gradle] script plugin.
 *
 * @see Etterlatte_rapids_and_rivers_gradle
 */
class Etterlatte_rapidsAndRiversPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Etterlatte_rapids_and_rivers_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
