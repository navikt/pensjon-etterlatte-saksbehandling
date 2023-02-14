package no.nav.etterlatte

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.etterlatte.hendelserpdl.AppConfig
import no.nav.etterlatte.hendelserpdl.LivsHendelserRapid
import no.nav.etterlatte.hendelserpdl.LyttPaaHendelser
import no.nav.etterlatte.hendelserpdl.leesah.LivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.module
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory
import kotlin.collections.set
import kotlin.system.exitProcess

var stream: LyttPaaHendelser? = null

fun main() {
    val env = System.getenv().toMutableMap()
    env["KAFKA_BOOTSTRAP_SERVERS"] = env["KAFKA_BROKERS"]
    env["NAV_TRUSTSTORE_PATH"] = env["KAFKA_TRUSTSTORE_PATH"]
    env["NAV_TRUSTSTORE_PASSWORD"] = env["KAFKA_CREDSTORE_PASSWORD"]
    env["KAFKA_KEYSTORE_PASSWORD"] = env["KAFKA_CREDSTORE_PASSWORD"]

    val logger = LoggerFactory.getLogger(Application::class.java)
    val pdlService by lazy {
        PdlService(pdlHttpClient(System.getenv()), "http://etterlatte-pdltjenester")
    }

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule(Application::module)
        .build()
        .apply {
            GlobalScope.launch {
                try {
                    stream =
                        LyttPaaHendelser(
                            LivetErEnStroemAvHendelser(AppConfig().env),
                            LivsHendelserRapid(this@apply),
                            pdlService
                        )

                    while (true) {
                        if (stream?.stopped == true) {
                            delay(200)
                        } else {
                            stream?.stream()
                        }
                    }
                } catch (e: Exception) {
                    logger.error("App avsluttet med en feil", e)
                    exitProcess(1)
                }
            }
        }.start()
}

fun pdlHttpClient(props: Map<String, String>) = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    install(Auth) {
        clientCredential {
            config = props.toMutableMap()
                .apply { put("AZURE_APP_OUTBOUND_SCOPE", requireNotNull(get("PDL_AZURE_SCOPE"))) }
        }
    }
    defaultRequest {
        header(HttpHeaders.XCorrelationId, getCorrelationId())
    }
}.also { Runtime.getRuntime().addShutdownHook(Thread { it.close() }) }