package no.nav.etterlatte.tilbakekreving.tilbakekreving.config

data class ApplicationProperties(
    val dbName: String,
    val dbHost: String,
    val dbPort: Int,
    val dbUsername: String,
    val dbPassword: String,
    val mqHost: String,
    val mqPort: Int,
    val mqQueueManager: String,
    val mqChannel: String,
    val mqSendQueue: String,
    val serviceUserUsername: String,
    val serviceUserPassword: String,
) {
    companion object {
        fun fromEnv(env: Map<String, String>) = env.run {
            ApplicationProperties(
                dbName = value("DB_DATABASE"),
                dbHost = value("DB_HOST"),
                dbPort = value("DB_PORT").toInt(),
                dbUsername = value("DB_USERNAME"),
                dbPassword = value("DB_PASSWORD"),
                mqHost = value("OPPDRAG_MQ_HOSTNAME"),
                mqPort = value("OPPDRAG_MQ_PORT").toInt(),
                mqQueueManager = value("OPPDRAG_MQ_MANAGER"),
                mqChannel = value("OPPDRAG_MQ_CHANNEL"),
                mqSendQueue = value("OPPDRAG_SEND_MQ_NAME"),
                serviceUserUsername = value("srvuser"),
                serviceUserPassword = value("srvpwd"),
            )
        }

        private fun Map<String, String>.value(property: String): String =
            requireNotNull(this[property]) { "Property $property was null" }
    }
}