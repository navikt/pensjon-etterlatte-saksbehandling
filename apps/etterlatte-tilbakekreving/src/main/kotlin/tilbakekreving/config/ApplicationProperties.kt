package no.nav.etterlatte.tilbakekreving.config

data class ApplicationProperties(
    val httpPort: Int,
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
    val behandlingScope: String,
    val proxyUrl: String,
    val proxyScope: String,
) {
    companion object {
        fun fromEnv(env: Map<String, String>) =
            env.run {
                ApplicationProperties(
                    httpPort = valueOrNull("HTTP_PORT")?.toInt() ?: 8080,
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
                    behandlingScope = value("ETTERLATTE_BEHANDLING_SCOPE"),
                    proxyUrl = value("ETTERLATTE_PROXY_URL"),
                    proxyScope = value("ETTERLATTE_PROXY_OUTBOUND_SCOPE"),
                )
            }

        private fun Map<String, String>.value(property: String): String = requireNotNull(this[property]) { "Property $property was null" }

        private fun Map<String, String>.valueOrNull(property: String): String? = this[property]
    }
}
