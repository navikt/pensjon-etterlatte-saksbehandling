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
                    dbName = getValue(DB_DATABASE),
                    dbHost = getValue(DB_HOST),
                    dbPort = getValue(DB_PORT).toInt(),
                    dbUsername = getValue(DB_USERNAME),
                    dbPassword = getValue(DB_PASSWORD),
                    mqHost = getValue(OPPDRAG_MQ_HOSTNAME),
                    mqPort = getValue(OPPDRAG_MQ_PORT).toInt(),
                    mqQueueManager = getValue(OPPDRAG_MQ_MANAGER),
                    mqChannel = getValue(OPPDRAG_MQ_CHANNEL),
                    mqSendQueue = getValue(OPPDRAG_SEND_MQ_NAME),
                    mqKvitteringQueue = getValue(OPPDRAG_KVITTERING_MQ_NAME),
                    mqAvstemmingQueue = getValue(OPPDRAG_AVSTEMMING_MQ_NAME),
                    serviceUserUsername = getValue(srvuser),
                    serviceUserPassword = getValue(srvpwd),
                    leaderElectorPath = getValue(ELECTOR_PATH),
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

    override fun name() = name
}
