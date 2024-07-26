package no.nav.etterlatte.utbetaling.config

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_DATABASE
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_HOST
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PASSWORD
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PORT
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_USERNAME
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
import no.nav.etterlatte.mq.MqKey.srvpwd
import no.nav.etterlatte.mq.MqKey.srvuser

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
    val konsistensavstemmingOMSEnabled: Boolean,
) {
    companion object {
        fun fromEnv(env: Miljoevariabler) =
            env.run {
                ApplicationProperties(
                    dbName = getValue(DB_DATABASE),
                    dbHost = getValue(DB_HOST),
                    dbPort = getValue(DB_PORT).toInt(),
                    dbUsername = getValue(DB_USERNAME),
                    dbPassword = getValue(DB_PASSWORD),
                    mqHost = value("OPPDRAG_MQ_HOSTNAME"),
                    mqPort = value("OPPDRAG_MQ_PORT").toInt(),
                    mqQueueManager = value("OPPDRAG_MQ_MANAGER"),
                    mqChannel = value("OPPDRAG_MQ_CHANNEL"),
                    mqSendQueue = value("OPPDRAG_SEND_MQ_NAME"),
                    mqKvitteringQueue = value("OPPDRAG_KVITTERING_MQ_NAME"),
                    mqAvstemmingQueue = value("OPPDRAG_AVSTEMMING_MQ_NAME"),
                    serviceUserUsername = getValue(srvuser),
                    serviceUserPassword = getValue(srvpwd),
                    leaderElectorPath = getValue(ELECTOR_PATH),
                    grensesnittavstemmingEnabled = value("GRENSESNITTAVSTEMMING_ENABLED").toBoolean(),
                    konsistensavstemmingEnabled = value("KONSISTENSAVSTEMMING_ENABLED").toBoolean(),
                    grensesnittavstemmingOMSEnabled = value("GRENSESNITTAVSTEMMING_OMS_ENABLED").toBoolean(),
                    konsistensavstemmingOMSEnabled = value("KONSISTENSAVSTEMMING_OMS_ENABLED").toBoolean(),
                )
            }
    }
}
