package no.nav.etterlatte.no.nav.etterlatte.testdata.features.dolly

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.no.nav.etterlatte.testdata.AuthorizationPluginDolly
import no.nav.etterlatte.rapidsandrivers.Behandlingssteg
import no.nav.etterlatte.testdata.dolly.DollyService
import no.nav.etterlatte.testdata.features.dolly.NySoeknadRequest
import no.nav.etterlatte.testdata.features.dolly.SoeknadResponse

fun Route.dollyRoute(dollyService: DollyService) {
    // Tiltenkt for eksterne bruk fra Dolly
    route("dolly/opprett-ytelse") {
        install(AuthorizationPluginDolly) {
            accessPolicyRolesEllerAdGrupper = setOf("les-bp-vedtak", "les-oms-vedtak")
            issuers = setOf(Issuer.AZURE.issuerName)
        }

        post {
            try {
                val nySoeknadRequest = call.receive<NySoeknadRequest>()
                val ytelse = nySoeknadRequest.type
                val behandlingssteg = Behandlingssteg.IVERKSATT
                val gjenlevende = nySoeknadRequest.gjenlevende
                val avdoed = nySoeknadRequest.avdoed
                val barnListe: List<String> = nySoeknadRequest.barn
                val soeker =
                    when (ytelse) {
                        // TODO
                        SoeknadType.BARNEPENSJON -> nySoeknadRequest.barn.first()
                        SoeknadType.OMSTILLINGSSTOENAD -> gjenlevende
                    }
                if (soeker == "" || barnListe.isEmpty() || avdoed == "") {
                    call.respond(HttpStatusCode.BadRequest, "Påkrevde felter mangler")
                }
                val request =
                    NySoeknadRequest(
                        ytelse,
                        avdoed,
                        gjenlevende,
                        barnListe,
                        soeker = soeker,
                    )

                val brukerId = brukerTokenInfo.ident()
                val noekkel = dollyService.sendSoeknad(request, brukerId, behandlingssteg)
                call.respond(SoeknadResponse(200, noekkel))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Noe gikk galt")
            }
        }
    }
}
