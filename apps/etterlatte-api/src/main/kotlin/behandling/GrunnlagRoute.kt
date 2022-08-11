package no.nav.etterlatte.behandling


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.getAccessToken
import no.nav.etterlatte.libs.common.person.Foedselsnummer

fun Route.grunnlagRoute(service: GrunnlagService) {

    route("/grunnlag") {
        post("/kommertilgode/{behandlingId}") {

            try {
                val behandlingId = call.parameters["behandlingId"]
                val body = call.receive<KommerBarnetTilgodeClientRequest>()

                if (behandlingId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else {
                    call.respond(
                        service.lagreResultatKommerBarnetTilgode(
                            behandlingId,
                            body.svar,
                            body.begrunnelse,
                            call.navIdent,
                            getAccessToken(call)
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.error("kommer barnet tilgode feilet", ex)
                throw ex
            }
        }

        post("/beregningsgrunnlag/{behandlingId}") {
            try {
                val behandlingId = call.parameters["behandlingId"]
                val body = call.receive<List<SoeskenMedIBeregning>>()

                if (behandlingId == null) { // todo ai: trekk ut
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else {
                    call.respond(
                        service.lagreSoeskenMedIBeregning(
                            behandlingId,
                            body.map { it.toDomain() },
                            call.navIdent,
                            getAccessToken(call)
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.error("beregningsgrunnlag feilet", ex)
                throw ex
            }
        }
    }
}

data class KommerBarnetTilgodeClientRequest(val svar: String, val begrunnelse: String)
private data class SoeskenMedIBeregning(val foedselsnummer: Foedselsnummer, val skalBrukes: Boolean) {
    fun toDomain() = no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning(this.foedselsnummer, this.skalBrukes)
}
