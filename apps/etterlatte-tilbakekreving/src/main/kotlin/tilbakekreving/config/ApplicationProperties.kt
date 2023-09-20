package no.nav.etterlatte.tilbakekreving.config

data class ApplicationProperties(
    val mqHost: String,
    val mqPort: Int,
    val mqQueueManager: String,
    val mqChannel: String,
    val mqKravgrunnlagQueue: String,
    val serviceUserUsername: String,
    val serviceUserPassword: String,
    val azureAppClientId: String,
    val azureAppJwk: String,
    val azureAppWellKnownUrl: String,
    val behandlingUrl: String,
    val behandlingScope: String
) {
    companion object {
        fun fromEnv(env: Map<String, String>) = env.run {
            ApplicationProperties(
                mqHost = value("MQ_HOSTNAME"),
                mqPort = value("MQ_PORT").toInt(),
                mqQueueManager = value("MQ_MANAGER"),
                mqChannel = value("MQ_CHANNEL"),
                mqKravgrunnlagQueue = value("KRAVGRUNNLAG_MQ_NAME"),
                serviceUserUsername = value("srvuser"),
                serviceUserPassword = value("srvpwd"),
                azureAppClientId = value("AZURE_APP_CLIENT_ID"),
                azureAppJwk = value("AZURE_APP_JWK"),
                azureAppWellKnownUrl = value("AZURE_APP_WELL_KNOWN_URL"),
                behandlingUrl = value("ETTERLATTE_BEHANDLING_URL"),
                behandlingScope = value("ETTERLATTE_BEHANDLING_CLIENT_ID")
            )
        }

        private fun Map<String, String>.value(property: String): String =
            requireNotNull(this[property]) { "Property $property was null" }
    }
}