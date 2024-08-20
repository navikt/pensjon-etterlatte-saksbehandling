package no.nav.etterlatte.tilbakekreving.config

import no.nav.etterlatte.EnvKey.HTTP_PORT
import no.nav.etterlatte.libs.common.EnvEnum
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_DATABASE
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_HOST
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_JDBC_URL
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PASSWORD
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_PORT
import no.nav.etterlatte.libs.database.DatabaseConfig.DB_USERNAME
import no.nav.etterlatte.libs.database.jdbcUrl
import no.nav.etterlatte.libs.ktor.AppConfig.DEV_MODE
import no.nav.etterlatte.libs.ktor.AzureEnums
import no.nav.etterlatte.mq.MqKey.MQ_CHANNEL
import no.nav.etterlatte.mq.MqKey.MQ_HOSTNAME
import no.nav.etterlatte.mq.MqKey.MQ_MANAGER
import no.nav.etterlatte.mq.MqKey.MQ_PORT
import no.nav.etterlatte.mq.MqKey.srvpwd
import no.nav.etterlatte.mq.MqKey.srvuser
import no.nav.etterlatte.tilbakekreving.config.TilbakekrevingKey.ETTERLATTE_BEHANDLING_SCOPE
import no.nav.etterlatte.tilbakekreving.config.TilbakekrevingKey.ETTERLATTE_BEHANDLING_URL
import no.nav.etterlatte.tilbakekreving.config.TilbakekrevingKey.ETTERLATTE_PROXY_SCOPE
import no.nav.etterlatte.tilbakekreving.config.TilbakekrevingKey.ETTERLATTE_PROXY_URL
import no.nav.etterlatte.tilbakekreving.config.TilbakekrevingKey.KRAVGRUNNLAG_MQ_NAME

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
                    mqHost = requireEnvValue(MQ_HOSTNAME),
                    mqPort = requireEnvValue(MQ_PORT).toInt(),
                    mqQueueManager = requireEnvValue(MQ_MANAGER),
                    mqChannel = requireEnvValue(MQ_CHANNEL),
                    mqKravgrunnlagQueue = requireEnvValue(KRAVGRUNNLAG_MQ_NAME),
                    serviceUserUsername = requireEnvValue(srvuser),
                    serviceUserPassword = requireEnvValue(srvpwd),
                    jdbcUrl =
                        env[DB_JDBC_URL] ?: jdbcUrl(
                            requireEnvValue(DB_HOST),
                            requireEnvValue(DB_PORT).toInt(),
                            requireEnvValue(DB_DATABASE),
                        ),
                    dbUsername = requireEnvValue(DB_USERNAME),
                    dbPassword = requireEnvValue(DB_PASSWORD),
                    azureAppClientId = requireEnvValue(AzureEnums.AZURE_APP_CLIENT_ID),
                    azureAppJwk = requireEnvValue(AzureEnums.AZURE_APP_JWK),
                    azureAppWellKnownUrl = requireEnvValue(AzureEnums.AZURE_APP_WELL_KNOWN_URL),
                    behandlingUrl = requireEnvValue(ETTERLATTE_BEHANDLING_URL),
                    behandlingScope = requireEnvValue(ETTERLATTE_BEHANDLING_SCOPE),
                    proxyUrl = requireEnvValue(ETTERLATTE_PROXY_URL),
                    proxyScope = requireEnvValue(ETTERLATTE_PROXY_SCOPE),
                    devMode = get(DEV_MODE).toBoolean(),
                )
            }
    }
}

enum class TilbakekrevingKey : EnvEnum {
    ETTERLATTE_BEHANDLING_URL,
    ETTERLATTE_BEHANDLING_SCOPE,
    ETTERLATTE_PROXY_URL,
    ETTERLATTE_PROXY_SCOPE,
    KRAVGRUNNLAG_MQ_NAME,
    ;

    override fun key() = name
}
