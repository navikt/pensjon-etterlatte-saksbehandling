package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.grunnlag.NyePersonopplysninger
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.sak.TilgangServiceSjekker
import no.nav.etterlatte.tilgangsstyring.withFoedselsnummerInternal

fun Route.personRoute(
    grunnlagService: GrunnlagService,
    tilgangService: TilgangServiceSjekker,
) {
    route("/person") {
        post("/saker") {
            withFoedselsnummerInternal(tilgangService) { fnr ->
                val saksliste = grunnlagService.hentAlleSakerForFnr(fnr, brukerTokenInfo)
                call.respond(saksliste)
            }
        }

        post("/roller") {
            withFoedselsnummerInternal(tilgangService) { fnr ->
                val personMedSakOgRoller = grunnlagService.hentSakerOgRoller(fnr, brukerTokenInfo)
                call.respond(personMedSakOgRoller)
            }
        }

        post("/behandling/{$BEHANDLINGID_CALL_PARAMETER}/nye-opplysninger") {
            val nyePersonopplysninger = call.receive<NyePersonopplysninger>()

            grunnlagService.lagreNyePersonopplysninger(
                sakId = nyePersonopplysninger.sakId,
                behandlingId = behandlingId,
                fnr = nyePersonopplysninger.fnr,
                nyeOpplysninger = nyePersonopplysninger.opplysninger,
                brukerTokenInfo = brukerTokenInfo,
            )

            call.respond(HttpStatusCode.OK)
        }
    }
}
