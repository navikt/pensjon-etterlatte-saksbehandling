package no.nav.etterlatte.utbetaling.config

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
    val mqKvitteringQueue: String,
    val mqAvstemmingQueue: String,
    val serviceUserUsername: String,
    val serviceUserPassword: String,
    val leaderElectorPath: String,
    val grensesnittavstemmingEnabled: Boolean,
    val konsistensavstemmingEnabled: Boolean,
    val grensesnittavstemmingOMSEnabled: Boolean,
    val konsistensavstemmingOMSEnabled: Boolean
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
                mqKvitteringQueue = value("OPPDRAG_KVITTERING_MQ_NAME"),
                mqAvstemmingQueue = value("OPPDRAG_AVSTEMMING_MQ_NAME"),
                serviceUserUsername = value("srvuser"),
                serviceUserPassword = value("srvpwd"),
                leaderElectorPath = value("ELECTOR_PATH"),
                grensesnittavstemmingEnabled = value("GRENSESNITTAVSTEMMING_ENABLED").toBoolean(),
                konsistensavstemmingEnabled = value("KONSISTENSAVSTEMMING_ENABLED").toBoolean(),
                grensesnittavstemmingOMSEnabled = value("GRENSESNITTAVSTEMMING_OMS_ENABLED").toBoolean(),
                konsistensavstemmingOMSEnabled = value("KONSISTENSAVSTEMMING_OMS_ENABLED").toBoolean(),

            )
        }

        private fun Map<String, String>.value(property: String): String =
            requireNotNull(this[property]) { "Property $property was null" }
    }
}