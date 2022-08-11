package testdata.features.egendefinert

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.nav.etterlatte.*
import no.nav.etterlatte.batch.JsonMessage


object EgendefinertMeldingFeature: TestDataFeature{
    override val beskrivelse: String
        get() = "Post egendefinert melding"
    override val path: String
        get() = "egendefinert"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respondHtml {
                    this.head {
                        title { +"Post melding til Kafka" }
                    }
                    body {
                        form(action = "/$path", method = FormMethod.post) {
                            label {
                                htmlFor = "key"
                                +"Nøkkel:"
                            }
                            br { }
                            textInput {
                                name = "key"
                                id = "key"
                            }
                            br { }
                            label {
                                htmlFor = "json"
                                +"Melding:"
                            }
                            br { }
                            textArea {
                                name = "json"
                                id = "json"
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

                val offset = call.receiveParameters().let {
                    producer.publiser(
                        requireNotNull(it["key"]),
                        JsonMessage(requireNotNull(it["json"])).toJson(),
                        mapOf("NavIdent" to (navIdentFraToken()!!.toByteArray()))
                    )
                }
                logger.info("Publiserer melding med partisjon: ${offset.first} offset: ${offset.second}")

                call.respondHtml {
                    this.head {
                        title { +"Post melding til Kafka" }
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
                                    +"Post ny melding"
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