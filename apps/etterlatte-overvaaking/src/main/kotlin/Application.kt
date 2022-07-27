package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.helse.rapids_rivers.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.tokenValidationSupport
import java.time.LocalDateTime

val appMap: MutableMap<String, MutableMap<String, Pair<AppEvent?, PongEvent?>>> = mutableMapOf()
fun main() {
    ventPaaNettverk()

    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                install(Authentication) {
                    tokenValidationSupport(config = HoconApplicationConfig(ConfigFactory.load()))
                }
                routing {
                    get("/started") {
                        call.respondText("STARTED", ContentType.Text.Plain)
                    }

                    authenticate {
                        get("/") {
                            call.respondHtml {
                                this.head {
                                    title {
                                        +"Overvåking Etterlatte"
                                    }
                                }
                                body {
                                    h1 {
                                        +"Hello, ${navIdentFraToken()}"
                                    }
                                    appMap.forEach {
                                        h2 {
                                            +it.key
                                        }

                                        ul {

                                            it.value.forEach {
                                                li {
                                                    +("${it.key}: " + (it.value.first?.let { """Reported ${it.type} at ${it.opprettet}. """ }
                                                        ?: "") + (it.value.second?.let { """Responded to ping at ${it.opprettet}.""" }
                                                        ?: ""))
                                                }
                                            }

                                        }


                                    }
                                }
                            }

                        }
                    }

                }
            }
            .build()
            .also {
                AppEventRiver(it)
                PongListener(it)
            }
            .start()
    }
}

private fun ventPaaNettverk() {
    runBlocking { delay(5000) }
}

val appEventTypes = listOf("application_up", "application_ready", "application_down", "application_not_ready")

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()

internal class AppEventRiver(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, appEventTypes) }
            validate { it.interestedIn("@opprettet", "app_name", "instance_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        AppEvent(
            packet[eventNameKey].asText(),
            packet["instance_id"].asText(),
            packet["app_name"].asText(),
            LocalDateTime.parse(packet["@opprettet"].asText())
        ).also {
            appMap.computeIfAbsent(it.app) { mutableMapOf() }
                .compute(it.appinstance) { _, current: Pair<AppEvent?, PongEvent?>? -> Pair(it, current?.second) }
        }
    }
}

internal class PongListener(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue(eventNameKey, "pong") }
            validate { it.interestedIn("@id", "ping_time", "pong_time", "app_name", "instance_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        PongEvent(
            packet["instance_id"].asText(),
            packet["app_name"].asText(),
            LocalDateTime.parse(packet["pong_time"].asText())
        ).also {
            appMap.computeIfAbsent(it.app) { mutableMapOf() }
                .compute(it.appinstance) { _, current: Pair<AppEvent?, PongEvent?>? -> Pair(current?.first, it) }
        }
    }
}

interface Event {
    val type: String
    val appinstance: String
    val app: String
    val opprettet: LocalDateTime
}

class AppEvent(
    override val type: String,
    override val appinstance: String,
    override val app: String,
    override val opprettet: LocalDateTime
) : Event {
    override fun toString(): String {
        return """$opprettet: Instance $appinstance of app $app reporting: $type"""
    }
}

class PongEvent(
    override val appinstance: String,
    override val app: String,
    override val opprettet: LocalDateTime,
) : Event by AppEvent("pong", appinstance, app, opprettet)
