package no.nav.etterlatte.tilbakekreving.config

import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_DATABASE
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_HOST
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_JDBC_URL
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PASSWORD
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PORT
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_USERNAME
import no.nav.etterlatte.libs.database.jdbcUrl
import no.nav.etterlatte.libs.ktor.AppConfig.HTTP_PORT
import no.nav.etterlatte.libs.ktor.AzureEnums
import no.nav.etterlatte.mq.MqKey.MQ_CHANNEL
import no.nav.etterlatte.mq.MqKey.MQ_HOSTNAME
import no.nav.etterlatte.mq.MqKey.MQ_MANAGER
import no.nav.etterlatte.mq.MqKey.MQ_PORT
import no.nav.etterlatte.mq.MqKey.srvpwd
import no.nav.etterlatte.mq.MqKey.srvuser

data class ApplicationProperties(
    val httpPort: Int,
    val mqHost: String,
    val mqPort: Int,
    val mqQueueManager: String,
    val mqChannel: String,
    val mqKravgrunnlagQueue: String,
    val serviceUserUsername: String,
    val serviceUserPassword: String,
    val jdbcUrl: String,
    val dbUsername: String,
    val dbPassword: String,
    val azureAppClientId: String,
    val azureAppJwk: String,
    val azureAppWellKnownUrl: String,
    val behandlingUrl: String,
    val behandlingScope: String,
    val proxyUrl: String,
    val proxyScope: String,
    val devMode: Boolean,
) {
    companion object {
        fun fromEnv(env: Miljoevariabler) =
            env.run {
                ApplicationProperties(
                    httpPort = get(HTTP_PORT)?.toInt() ?: 8080,
                    mqHost = getValue(MQ_HOSTNAME),
                    mqPort = getValue(MQ_PORT).toInt(),
                    mqQueueManager = getValue(MQ_MANAGER),
                    mqChannel = getValue(MQ_CHANNEL),
                    mqKravgrunnlagQueue = value("KRAVGRUNNLAG_MQ_NAME"),
                    serviceUserUsername = getValue(srvuser),
                    serviceUserPassword = getValue(srvpwd),
                    jdbcUrl =
                        env[DB_JDBC_URL] ?: jdbcUrl(
                            getValue(DB_HOST),
                            getValue(DB_PORT).toInt(),
                            getValue(DB_DATABASE),
                        ),
                    dbUsername = getValue(DB_USERNAME),
                    dbPassword = getValue(DB_PASSWORD),
                    azureAppClientId = getValue(AzureEnums.AZURE_APP_CLIENT_ID),
                    azureAppJwk = getValue(AzureEnums.AZURE_APP_JWK),
                    azureAppWellKnownUrl = getValue(AzureEnums.AZURE_APP_WELL_KNOWN_URL),
                    behandlingUrl = value("ETTERLATTE_BEHANDLING_URL"),
                    behandlingScope = value("ETTERLATTE_BEHANDLING_SCOPE"),
                    proxyUrl = value("ETTERLATTE_PROXY_URL"),
                    proxyScope = value("ETTERLATTE_PROXY_SCOPE"),
                    devMode = get("DEV_MODE").toBoolean(),
                )
            }
    }
}
