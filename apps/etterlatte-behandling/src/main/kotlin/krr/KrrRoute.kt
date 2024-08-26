package no.nav.etterlatte.krr

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.sak.TilgangService
import no.nav.etterlatte.tilgangsstyring.withFoedselsnummerInternal

/**
 * Endepunkter for KRR (Kontakt- og Reservasjonsregisteret)
 * Brukes av frontend for å vise sb hvilke preferanser brukeren har, slik at de kan få en indikasjon på hvordan
 * vi kommuniserer med brukeren
 **/
fun Route.krrRoute(
    tilgangService: TilgangService,
    klient: KrrKlient,
) {
    route("/api/krr") {
        post {
            withFoedselsnummerInternal(tilgangService) { fnr ->
                val response = klient.hentDigitalKontaktinformasjon(fnr.value)

                call.respond(response ?: HttpStatusCode.NoContent)
            }
        }
    }
}
