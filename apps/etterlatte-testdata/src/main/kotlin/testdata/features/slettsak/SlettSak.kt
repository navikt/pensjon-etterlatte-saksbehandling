package testdata.features

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.batch.JsonMessage
import no.nav.etterlatte.logger
import no.nav.etterlatte.navIdentFraToken
import no.nav.etterlatte.producer

object SlettsakFeature: TestDataFeature {
    override val beskrivelse: String
        get() = "Slett sak"
    override val path: String
        get() = "slettsak"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respondHtml {
                    this.head {
                        title { +"Post vedlikeholdsmelding for å slette sak" }
                    }
                    body {
                        form(action = "/$path", method = FormMethod.post) {
                            label {
                                htmlFor = "sak"
                                +"SakId:"
                            }
                            br { }
                            textInput {
                                name = "sak"
                                id = "sak"
                            }
                            br { }
                            submitInput()
                        }
                        ul {
                            li {
                                a {
                                    href = "/"
                                    +"Tilbake til hovedmeny"
                                }
                            }
                        }
                    }
                }
            }
            post {

                val offset = call.receiveParameters()
                    .let { requireNotNull(it["sak"]).toLong()}
                    .let {
                    producer.publiser(
                        it.toString(),
                        JsonMessage.newMessage(
                            mapOf(
                                "@event_name" to "VEDLIKEHOLD:SLETT_SAK",
                                "sakId" to it
                            )
                            ).toJson(),
                        mapOf("NavIdent" to (navIdentFraToken()!!.toByteArray()))
                    )
                }
                logger.info("Publiserer melding med partisjon: ${offset.first} offset: ${offset.second}")

                call.respondHtml {
                    this.head {
                        title { +"Post vedlikeholdsmelding for å slette sak" }
                    }
                    body {
                        h3 {
                            +"Melding postet!"
                        }
                        p { +"Partisjon: ${offset.first} Offset: ${offset.second}" }
                        br {}
                        ul {
                            li {
                                a {
                                    href = "/$path"
                                    +"Slett en sak til"
                                }
                            }
                            li {
                                a {
                                    href = "/"
                                    +"Tilbake til hovedmeny"
                                }
                            }
                        }
                    }
                }

            }
        }

}