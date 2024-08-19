package no.nav.etterlatte.utbetaling.config

import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_DATABASE
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_HOST
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PASSWORD
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PORT
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_USERNAME
import no.nav.etterlatte.libs.ktor.AppConfig.ELECTOR_PATH
import no.nav.etterlatte.mq.MqKey.srvpwd
import no.nav.etterlatte.mq.MqKey.srvuser
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.GRENSESNITTAVSTEMMING_ENABLED
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.GRENSESNITTAVSTEMMING_OMS_ENABLED
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.KONSISTENSAVSTEMMING_ENABLED
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.KONSISTENSAVSTEMMING_OMS_ENABLED
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.OPPDRAG_AVSTEMMING_MQ_NAME
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.OPPDRAG_KVITTERING_MQ_NAME
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.OPPDRAG_MQ_CHANNEL
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.OPPDRAG_MQ_HOSTNAME
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.OPPDRAG_MQ_MANAGER
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.OPPDRAG_MQ_PORT
import no.nav.etterlatte.utbetaling.config.UtbetalingKey.OPPDRAG_SEND_MQ_NAME

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
                    dbName = requireEnvValue(DB_DATABASE),
                    dbHost = requireEnvValue(DB_HOST),
                    dbPort = requireEnvValue(DB_PORT).toInt(),
                    dbUsername = requireEnvValue(DB_USERNAME),
                    dbPassword = requireEnvValue(DB_PASSWORD),
                    mqHost = requireEnvValue(OPPDRAG_MQ_HOSTNAME),
                    mqPort = requireEnvValue(OPPDRAG_MQ_PORT).toInt(),
                    mqQueueManager = requireEnvValue(OPPDRAG_MQ_MANAGER),
                    mqChannel = requireEnvValue(OPPDRAG_MQ_CHANNEL),
                    mqSendQueue = requireEnvValue(OPPDRAG_SEND_MQ_NAME),
                    mqKvitteringQueue = requireEnvValue(OPPDRAG_KVITTERING_MQ_NAME),
                    mqAvstemmingQueue = requireEnvValue(OPPDRAG_AVSTEMMING_MQ_NAME),
                    serviceUserUsername = requireEnvValue(srvuser),
                    serviceUserPassword = requireEnvValue(srvpwd),
                    leaderElectorPath = requireEnvValue(ELECTOR_PATH),
                    grensesnittavstemmingEnabled = getValue(GRENSESNITTAVSTEMMING_ENABLED).toBoolean(),
                    konsistensavstemmingEnabled = getValue(KONSISTENSAVSTEMMING_ENABLED).toBoolean(),
                    grensesnittavstemmingOMSEnabled = getValue(GRENSESNITTAVSTEMMING_OMS_ENABLED).toBoolean(),
                    konsistensavstemmingOMSEnabled = getValue(KONSISTENSAVSTEMMING_OMS_ENABLED).toBoolean(),
                )
            }
    }
}

enum class UtbetalingKey : EnvEnum {
    OPPDRAG_MQ_HOSTNAME,
    OPPDRAG_MQ_PORT,
    OPPDRAG_MQ_MANAGER,
    OPPDRAG_MQ_CHANNEL,
    OPPDRAG_SEND_MQ_NAME,
    OPPDRAG_KVITTERING_MQ_NAME,
    OPPDRAG_AVSTEMMING_MQ_NAME,
    GRENSESNITTAVSTEMMING_ENABLED,
    KONSISTENSAVSTEMMING_ENABLED,
    GRENSESNITTAVSTEMMING_OMS_ENABLED,
    KONSISTENSAVSTEMMING_OMS_ENABLED,
    ;

    override fun key() = name
}
