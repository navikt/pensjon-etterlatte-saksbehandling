package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.config.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import no.nav.helse.rapids_rivers.*
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import no.nav.security.token.support.ktor.tokenValidationSupport

var appEvents = emptyList<String>()

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
                    get("/started"){
                        call.respondText("STARTED", ContentType.Text.Plain)
                    }

                    authenticate {
                        get ("/"){
                            call.respondHtml {
                                this.head {
                                    title {
                                        +"Overvåking Etterlatte. Logget inn som ${navIdentFraToken()}"
                                    }
                                }
                                body {
                                    h1 {
                                        +"Hello"
                                    }
                                    ul {
                                        appEvents.forEach {
                                            li {
                                                +it
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
            .also { AppEventRiver(it) }
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
            validate { it.demandAny("@event_name", appEventTypes) }
            validate { it.interestedIn("@opprettet", "app_name", "instance_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        appEvents = appEvents.plus("""${packet["@opprettet"]}: Instance ${packet["instance_id"]} of app ${packet["app_name"]} reporting:  ${packet["@event_name"]}""").let { if(it.size > 50) it.drop(it.size -50) else it }
    }
}