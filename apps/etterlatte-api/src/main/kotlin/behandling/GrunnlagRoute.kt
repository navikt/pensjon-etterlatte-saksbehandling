package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
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
                        service.upsertAvdoedMedlemskapPeriode(
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

        delete("/saksbehandler/periode/{behandlingId}/{saksbehandlerPeriodeId}") {
            try {
                val behandlingId = call.parameters["behandlingId"]
                val saksbehandlerPeriodeId = call.parameters["saksbehandlerPeriodeId"]

                if (behandlingId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else if (saksbehandlerPeriodeId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("saksbehandlerPeriodeId mangler")
                } else {
                    call.respond(
                        service.slettAvdoedMedlemskapPeriode(
                            behandlingId = behandlingId,
                            saksbehandlerPeriodeId = saksbehandlerPeriodeId,
                            saksbehandlerId = call.navIdent,
                            getAccessToken(call)
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.error("Sletting av medlemskapsperiode feilet", ex)
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
        periodeType = renameEnum(this.periode.periodeType.uppercase()).let { PeriodeType.valueOf(it) },
        id = if (periode.id.isNullOrEmpty()) UUID.randomUUID().toString() else periode.id,
        kilde = Grunnlagsopplysning.Saksbehandler(saksbehandlerId, Instant.now()),
        arbeidsgiver = periode.arbeidsgiver,
        stillingsprosent = periode.stillingsprosent?.replace("%", ""),
        begrunnelse = periode.begrunnelse,
        oppgittKilde = periode.kilde,
        fraDato = periode.fraDato,
        tilDato = periode.tilDato
    )

    private fun renameEnum(name: String): String {
        return if (name == "UFØRETRYGD") {
            "UFOERETRYGD"
        } else {
            name
        }
    }
}

data class AvdoedesMedlemskapsperiodeClientRequest(
    val id: String?,
    val periodeType: String,
    val arbeidsgiver: String?,
    val stillingsprosent: String?,
    val begrunnelse: String?,
    val kilde: String,
    val fraDato: LocalDate,
    val tilDato: LocalDate
)

private data class SoeskenMedIBeregning(val foedselsnummer: Foedselsnummer, val skalBrukes: Boolean) {
    fun toDomain() = no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning(
        this.foedselsnummer,
        this.skalBrukes
    )
}