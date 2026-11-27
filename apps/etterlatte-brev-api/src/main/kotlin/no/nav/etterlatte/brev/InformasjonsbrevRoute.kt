package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readAvailable
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.parseUuidParameter
import no.nav.etterlatte.libs.ktor.route.withSakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import java.io.ByteArrayOutputStream

fun Route.informasjonsbrevRoute(
    service: InformasjonsbrevService,
    tilgangssjekker: Tilgangssjekker,
    behandlingService: BehandlingService,
    konfigurasjon: BrevBatchKonfigurasjon,
) {
    route("brev/strukturert/sak/{sakId}/utsending/{utsendingsId}") {
        post {
            konfigurasjon.kontrollerTilgang(brukerTokenInfo, true)
            withSakId(tilgangssjekker, skrivetilgang = true) { sakId ->
                behandlingService.hentSak(sakId, brukerTokenInfo)
                val kanal = call.receiveChannel()
                val buffer = ByteArray(8192)
                val bytes = ByteArrayOutputStream()
                while (true) {
                    val antall = kanal.readAvailable(buffer, 0, buffer.size)
                    if (antall < 0) break
                    if (bytes.size() + antall > 256 * 1024) {
                        throw ForespoerselException(413, "BREV_REQUEST_FOR_STOR", "Requesten overstiger 256 KiB.")
                    }
                    bytes.write(buffer, 0, antall)
                }
                val request =
                    try {
                        deserialize<StrukturertInformasjonsbrevRequest>(bytes.toString(Charsets.UTF_8))
                    } catch (_: Exception) {
                        throw UgyldigForespoerselException("UGYLDIG_BREV_REQUEST", "Requesten har ugyldig JSON eller brevdata.")
                    }
                call.respond(
                    HttpStatusCode.Created,
                    service.opprett(sakId, call.parameters.parseUuidParameter("utsendingsId"), request, brukerTokenInfo),
                )
            }
        }
        get {
            konfigurasjon.kontrollerTilgang(brukerTokenInfo, false)
            withSakId(tilgangssjekker) { sakId ->
                behandlingService.hentSak(sakId, brukerTokenInfo)
                val resultat = service.hent(sakId, call.parameters.parseUuidParameter("utsendingsId"), brukerTokenInfo)
                if (resultat == null) call.respond(HttpStatusCode.NoContent) else call.respond(resultat)
            }
        }
        post("send") {
            konfigurasjon.kontrollerTilgang(brukerTokenInfo, true)
            withSakId(tilgangssjekker, skrivetilgang = true) { sakId ->
                behandlingService.hentSak(sakId, brukerTokenInfo)
                call.respond(service.send(sakId, call.parameters.parseUuidParameter("utsendingsId"), brukerTokenInfo))
            }
        }
    }
}
