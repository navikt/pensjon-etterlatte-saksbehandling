import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.jackson.jackson

internal interface WireMockBase {
    companion object {
        val mockServer: WireMockServer by lazy {
            WireMockServer(WireMockConfiguration.options().dynamicPort()).also {
                it.start()
            }
        }

        val mockHttpClient =
            HttpClient {
                expectSuccess = true
                defaultRequest {
                    url("http://localhost:${mockServer.port()}")
                }
                install(ContentNegotiation) {
                    jackson {
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    }
                }
            }
    }
}
