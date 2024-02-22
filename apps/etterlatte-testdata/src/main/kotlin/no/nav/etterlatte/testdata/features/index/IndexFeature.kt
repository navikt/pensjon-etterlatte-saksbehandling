package no.nav.etterlatte.testdata.features.index

import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.features
import no.nav.etterlatte.navIdentFraToken

object IndexFeature : TestDataFeature {
    override val beskrivelse: String
        get() = ""
    override val path: String
        get() = "/"
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "index.hbs",
                        mapOf(
                            "navIdent" to (navIdentFraToken() ?: "Anonym"),
                            "features" to
                                features.filter { it != IndexFeature }.map {
                                    mapOf(
                                        "path" to it.path,
                                        "beskrivelse" to it.beskrivelse,
                                    )
                                },
                        ),
                    ),
                )
            }
        }
}
