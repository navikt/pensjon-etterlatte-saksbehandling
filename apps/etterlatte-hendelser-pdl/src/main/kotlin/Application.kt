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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.etterlatte.hendelserpdl.LivsHendelserRapid
import no.nav.etterlatte.hendelserpdl.LyttPaaHendelser
import no.nav.etterlatte.hendelserpdl.leesah.LivetErEnStroemAvHendelser
import no.nav.etterlatte.hendelserpdl.module
import no.nav.etterlatte.hendelserpdl.pdl.PdlService
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.security.ktor.clientCredential
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    val env = System.getenv().toMutableMap()

    val logger = LoggerFactory.getLogger(Application::class.java)
    val pdlService by lazy {
        PdlService(pdlHttpClient(env), "http://etterlatte-pdltjenester")
    }

    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
        .withKtorModule(Application::module)
        .build()
        .apply {
            GlobalScope.launch {
                try {
                    val stream =
                        LyttPaaHendelser(
                            LivetErEnStroemAvHendelser(env),
                            LivsHendelserRapid(this@apply),
                            pdlService
                        )

                    while (true) {
                        if (stream.stopped) {
                            delay(200)
                        } else {
                            stream.stream()
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