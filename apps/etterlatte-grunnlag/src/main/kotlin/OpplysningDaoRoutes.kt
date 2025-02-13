package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.tilSakId
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import java.util.UUID

fun Route.opplysningDaoRoutes(opplysningDao: OpplysningDao) {
    route("/dao") {
        get("/finnesGrunnlagForSak") {
            val sakId = call.request.queryParameters["sakId"]!!.tilSakId()

            val finnesGrunnlag = opplysningDao.finnesGrunnlagForSak(sakId)

            call.respond(finnesGrunnlag)
        }

        get("/hentAlleGrunnlagForSak") {
            val sakId = call.request.queryParameters["sakId"]!!.tilSakId()

            val alleGrunnlag = opplysningDao.hentAlleGrunnlagForSak(sakId)

            call.respond(alleGrunnlag)
        }

        get("/hentAlleGrunnlagForBehandling") {
            val behandlingId = UUID.fromString(call.request.queryParameters["behandlingId"]!!)

            val alleGrunnlag = opplysningDao.hentAlleGrunnlagForBehandling(behandlingId)

            call.respond(alleGrunnlag)
        }

        get("/hentGrunnlagAvTypeForBehandling") {
            val behandlingId = UUID.fromString(call.request.queryParameters["behandlingId"]!!)
            val typer =
                call.request.queryParameters
                    .getAll("type")!!
                    .map(Opplysningstype::valueOf)

            val grunnlag = opplysningDao.hentGrunnlagAvTypeForBehandling(behandlingId, *typer.toTypedArray())

            call.respond(grunnlag)
        }

        get("/finnHendelserIGrunnlag") {
            val sakId = call.request.queryParameters["sakId"]!!.tilSakId()

            val hendelser = opplysningDao.finnHendelserIGrunnlag(sakId)

            call.respond(hendelser)
        }

        get("/finnNyesteGrunnlagForSak") {
            val sakId = call.request.queryParameters["sakId"]!!.tilSakId()
            val opplysningstype = Opplysningstype.valueOf(call.request.queryParameters["opplysningstype"]!!)

            val nyesteGrunnlag = opplysningDao.finnNyesteGrunnlagForSak(sakId, opplysningstype)

            call.respond(nyesteGrunnlag ?: HttpStatusCode.OK)
        }

        get("/finnNyesteGrunnlagForBehandling") {
            val behandlingId = UUID.fromString(call.request.queryParameters["behandlingId"]!!)
            val opplysningstype = Opplysningstype.valueOf(call.request.queryParameters["opplysningstype"]!!)

            val nyesteGrunnlag = opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, opplysningstype)

            call.respond(nyesteGrunnlag ?: HttpStatusCode.OK)
        }

        post("/leggOpplysningTilGrunnlag") {
            val sakId = call.request.queryParameters["sakId"]!!.tilSakId()

            val request = call.receive<NyOpplysningRequest>()

            opplysningDao.leggOpplysningTilGrunnlag(sakId, request.opplysning, request.fnr)

            call.respond(HttpStatusCode.OK)
        }

        get("/oppdaterVersjonForBehandling") {
            val sakId = call.request.queryParameters["sakId"]!!.tilSakId()
            val behandlingId = UUID.fromString(call.request.queryParameters["behandlingId"]!!)
            val hendelsesnummer = call.request.queryParameters["hendelsesnummer"]!!.toLong()

            opplysningDao.oppdaterVersjonForBehandling(behandlingId, sakId, hendelsesnummer)

            call.respond(HttpStatusCode.OK)
        }

        get("/laasGrunnlagVersjonForBehandling") {
            val behandlingId = UUID.fromString(call.request.queryParameters["behandlingId"]!!)

            opplysningDao.laasGrunnlagVersjonForBehandling(behandlingId)

            call.respond(HttpStatusCode.OK)
        }

        post("/finnAllePersongalleriHvorPersonFinnes") {
            val foedselsnummer = call.receive<FoedselsnummerDTO>()

            val grunnlagshendelser =
                opplysningDao.finnAllePersongalleriHvorPersonFinnes(Folkeregisteridentifikator.of(foedselsnummer.foedselsnummer))

            call.respond(grunnlagshendelser)
        }

        post("/finnAlleSakerForPerson") {
            val foedselsnummer = call.receive<FoedselsnummerDTO>()

            val saker =
                opplysningDao.finnAlleSakerForPerson(Folkeregisteridentifikator.of(foedselsnummer.foedselsnummer))

            call.respond(saker)
        }

        get("/hentBehandlingVersjon") {
            val behandlingId = UUID.fromString(call.request.queryParameters["behandlingId"]!!)

            val versjon = opplysningDao.hentBehandlingVersjon(behandlingId)

            call.respond(versjon ?: HttpStatusCode.OK)
        }
    }
}

data class NyOpplysningRequest(
    val fnr: Folkeregisteridentifikator? = null,
    val opplysning: Grunnlagsopplysning<JsonNode>,
)
