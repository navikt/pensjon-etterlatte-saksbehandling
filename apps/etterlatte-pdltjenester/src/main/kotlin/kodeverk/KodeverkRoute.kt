package no.nav.etterlatte.kodeverk
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.routing.get
import no.nav.etterlatte.common.toJson


fun Route.kodeverkApi(service: KodeverkService) {
    route("kodeverk") {
        get("alleland") {
            val landListe = service.hentAlleLand()
            call.respondText(landListe.toJson())
        }
    }
}