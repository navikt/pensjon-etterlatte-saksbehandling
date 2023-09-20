package no.nav.etterlatte.libs.regler

import java.util.Properties

object Properties {
    private val versionProperties = Properties()

    val reglerVersjon: String
        get() {
            return versionProperties.getProperty("version") ?: "unknown"
        }

    init {
        val versionPropertiesFile = this.javaClass.getResourceAsStream("/version.properties")
        versionProperties.load(versionPropertiesFile)
    }
}
