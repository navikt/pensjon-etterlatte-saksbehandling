package no.nav.etterlatte.testdata.features.index

import io.ktor.server.application.call
import io.ktor.server.mustache.MustacheContent
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.etterlatte.TestDataFeature
import no.nav.etterlatte.features
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.testdata.harGyldigAdGruppe

object IndexFeature : TestDataFeature {
    override val beskrivelse: String
        get() = ""
    override val path: String
        get() = "/"
    override val kunEtterlatte: Boolean
        get() = false
    override val routes: Route.() -> Unit
        get() = {
            get {
                call.respond(
                    MustacheContent(
                        "index.hbs",
                        mapOf(
                            "navIdent" to (brukerTokenInfo.ident()),
                            "features" to
                                features
                                    .filter { it != IndexFeature }
                                    .filter {
                                        if (it.kunEtterlatte) {
                                            harGyldigAdGruppe()
                                        } else {
                                            true
                                        }
                                    }.map {
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
