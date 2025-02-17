package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import java.util.UUID

fun Route.behandlingGrunnlagRoute(grunnlagService: GrunnlagService) {
    route("/behandling/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            val opplysningsgrunnlag =
                grunnlagService.hentOpplysningsgrunnlag(behandlingId)
                    ?: throw GenerellIkkeFunnetException()
            call.respond(opplysningsgrunnlag)
        }

        get("{opplysningType}") {
            val opplysningstype = Opplysningstype.valueOf(call.parameters["opplysningType"].toString())
            val grunnlag = grunnlagService.hentGrunnlagAvType(behandlingId, opplysningstype)

            if (grunnlag != null) {
                call.respond(grunnlag)
            } else if (opplysningstype == Opplysningstype.SOESKEN_I_BEREGNINGEN) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                throw GenerellIkkeFunnetException()
            }
        }

        get("revurdering/${Opplysningstype.HISTORISK_FORELDREANSVAR}") {
            val historisk =
                grunnlagService.hentHistoriskForeldreansvar(behandlingId)
                    ?: throw GenerellIkkeFunnetException()
            call.respond(historisk)
        }

        post("/nye-opplysninger") {
            val opplysningsbehov = call.receive<NyeSaksopplysninger>()
            grunnlagService.lagreNyeSaksopplysninger(
                opplysningsbehov.sakId,
                behandlingId,
                opplysningsbehov.opplysninger,
            )
            call.respond(HttpStatusCode.OK)
        }

        post("/laas") {
            grunnlagService.laasVersjonForBehandling(behandlingId)
            call.respond(HttpStatusCode.OK)
        }

        post("/laas-til-behandling/{behandlingIdLaasesTil}") {
            val idSomLaasesTil = UUID.fromString(call.parameters["behandlingIdLaasesTil"].toString())
            grunnlagService.laasTilVersjonForBehandling(
                skalLaasesId = behandlingId,
                idLaasesTil = idSomLaasesTil,
            )
            call.respond(HttpStatusCode.OK)
        }

        post("/opprett-grunnlag") {
            val opplysningsbehov = call.receive<Opplysningsbehov>()
            grunnlagService.opprettGrunnlag(behandlingId, opplysningsbehov)
            call.respond(HttpStatusCode.OK)
        }

        post("oppdater-grunnlag") {
            val request = call.receive<OppdaterGrunnlagRequest>()
            grunnlagService.oppdaterGrunnlag(behandlingId, request.sakId, request.sakType)
            call.respond(HttpStatusCode.OK)
        }

        get("opplysning/persongalleri-samsvar") {
            val persongalleri = grunnlagService.hentPersongalleriSamsvar(behandlingId)
            call.respond(persongalleri)
        }

        get("personopplysninger") {
            val sakstype = SakType.valueOf(call.parameters["sakType"].toString())
            val grunnlagsopplysninger =
                grunnlagService.hentPersonopplysninger(behandlingId, sakstype)
            call.respond(grunnlagsopplysninger)
        }
    }
}
