package testdata.features.index

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.features
import no.nav.etterlatte.navIdentFraToken

object IndexFeature: TestDataFeature {
    override val beskrivelse: String
        get() = ""
    override val path: String
        get() = "/"
    override val routes: Route.() -> Unit
        get() = {
            get {
            call.respondHtml {
                this.head {
                    title { +"TestData" }
                }
                body {
                    h2 {
                        +"Etterlatte testdata"
                    }
                    p {
                        +"Innlogget som ${navIdentFraToken() ?: "Anonym"}"
                    }
                    h4 {
                        +"Meny"
                    }
                    ul {
                        features.filter { it != IndexFeature }.forEach{
                            li {
                                a {
                                    href = "/${it.path}"
                                    +it.beskrivelse
                                }
                            }
                        }
                    }
                }
            }
        }}
}