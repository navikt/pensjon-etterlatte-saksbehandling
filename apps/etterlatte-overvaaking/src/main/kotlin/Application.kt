package no.nav.etterlatte

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import kotlinx.html.*
import no.nav.helse.rapids_rivers.*

var appEvents = emptyList<String>()

fun main() {
    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env))
            .withKtorModule {
                routing {
                    get ("/"){
                        call.respondHtml {
                            this.head {
                                title {
                                    +"Overvåking Etterlatte"
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
            .build()
            .also { AppEventRiver(it) }
            .start()
    }
}

val appEventTypes = listOf("application_up", "application_ready", "application_down", "application_not_ready")

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