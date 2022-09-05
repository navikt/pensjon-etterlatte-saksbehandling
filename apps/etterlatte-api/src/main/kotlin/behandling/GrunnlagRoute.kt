package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.getAccessToken
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.PeriodeType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperiode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*

fun Route.grunnlagRoute(service: GrunnlagService) {
    val logger = LoggerFactory.getLogger(GrunnlagService::class.java)

    route("/grunnlag") {
        post("/saksbehandler/periode/{behandlingId}") {
            try {
                val behandlingId = call.parameters["behandlingId"]
                val body = call.receive<MedlemskapsPeriodeClientRequest>()

                if (behandlingId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else {
                    call.respond(
                        service.lagreAvdoedMedlemskapPeriode(
                            behandlingId,
                            body.toDomain(call.navIdent),
                            call.navIdent,
                            getAccessToken(call)
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.error("lagring av medlemskapsperiode feilet", ex)
                throw ex
            }
        }

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

data class MedlemskapsPeriodeClientRequest(val periode: AvdoedesMedlemskapsperiodeClientRequest) {

    fun toDomain(saksbehandlerId: String) = SaksbehandlerMedlemskapsperiode(
        periodeType = PeriodeType.valueOf(this.periode.periodeType.uppercase()),
        id = periode.id ?: UUID.randomUUID().toString(),
        kilde = Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
        arbeidsgiver = periode.arbeidsgiver,
        stillingsprosent = periode.stillingsprosent,
        begrunnelse = this.periode.begrunnelse,
        oppgittKilde = this.periode.oppgittKilde,
        fraDato = this.periode.fraDato,
        tilDato = this.periode.tilDato
    )
}

data class AvdoedesMedlemskapsperiodeClientRequest(
    val id: String?,
    val periodeType: String,
    val arbeidsgiver: String?,
    val stillingsprosent: String?,
    val begrunnelse: String?,
    val oppgittKilde: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate
)

private data class KommerBarnetTilgodeClientRequest(val svar: String, val begrunnelse: String)
private data class SoeskenMedIBeregning(val foedselsnummer: Foedselsnummer, val skalBrukes: Boolean) {
    fun toDomain() = no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning(
        this.foedselsnummer,
        this.skalBrukes
    )
}