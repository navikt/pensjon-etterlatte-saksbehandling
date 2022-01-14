package no.nav.etterlatte.libs.ktorobo

import com.fasterxml.jackson.annotation.JsonProperty
import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EmptyConfiguration
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking

private val config = ConfigurationProperties.systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromOptionalResource("application-local.secrets.properties") overriding
        ConfigurationProperties.fromResource("application.properties")

data class Configuration(
    val application: Application = Application(),
    val azureAd: AzureAd = AzureAd(),
    val downstream: Downstream = Downstream()
) {
    data class Application(
        val port: Int = config[Key("application.port", intType)],
        val name: String = config[Key("application.name", stringType)]
    )

    data class AzureAd(
        val clientId: String = config[Key("azure.app.client.id", stringType)],
        val clientSecret: String = config[Key("azure.app.client.secret", stringType)],
        val wellKnownConfigurationUrl: String = config[Key("azure.app.well.known.url", stringType)],
        val openIdConfiguration: AzureAdOpenIdConfiguration = runBlocking {
            defaultHttpClient.get(wellKnownConfigurationUrl)
        }
    )

    data class Downstream(
        val clientId: String = config[Key("downstream.client.id", stringType)],
        val resourceUrl: String = config[Key("downstream.resource.url", stringType)]
    )
}

data class AzureAdOpenIdConfiguration(
    @JsonProperty("jwks_uri")
    val jwksUri: String,
    @JsonProperty("issuer")
    val issuer: String,
    @JsonProperty("token_endpoint")
    val tokenEndpoint: String,
    @JsonProperty("authorization_endpoint")
    val authorizationEndpoint: String
)

private fun ConfigurationProperties.Companion.fromOptionalResource(resourceName: String): Configuration =
    ClassLoader.getSystemClassLoader().getResource(resourceName)?.let {
        fromResource(resourceName)
    } ?: EmptyConfiguration