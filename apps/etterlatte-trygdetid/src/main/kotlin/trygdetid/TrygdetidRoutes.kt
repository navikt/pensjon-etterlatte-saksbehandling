package no.nav.etterlatte.trygdetid

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import java.time.LocalDate
import java.util.*

fun Route.trygdetid(trygdetidService: TrygdetidService, behandlingKlient: BehandlingKlient) {
    route("/api/trygdetid/{$BEHANDLINGSID_CALL_PARAMETER}") {
        val logger = application.log

        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter trygdetid for behandling $behandlingsId")
                val trygdetid = trygdetidService.hentTrygdetid(behandlingsId)
                if (trygdetid != null) {
                    call.respond(trygdetid.toDto())
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        post {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter trygdetid for behandling $behandlingsId")
                val trygdetid = trygdetidService.opprettTrygdetid(behandlingsId, bruker)
                call.respond(trygdetid.toDto())
            }
        }

        post("/grunnlag") {
            withBehandlingId(behandlingKlient) {
                logger.info("Legger til trygdetidsgrunnlag for behandling $behandlingsId")
                val trygdetidgrunnlagDto = call.receive<TrygdetidGrunnlagDto>()
                val trygdetid =
                    trygdetidService.lagreTrygdetidGrunnlag(
                        behandlingsId,
                        bruker,
                        trygdetidgrunnlagDto.toTrygdetidGrunnlag()
                    )
                call.respond(trygdetid.toDto())
            }
        }

        post("/beregnet") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppdaterer beregnet trygdetid for behandling $behandlingsId")
                val beregnetTrygdetid = call.receive<BeregnetTrygdetidDto>()
                val trygdetid =
                    trygdetidService.lagreBeregnetTrygdetid(
                        behandlingsId,
                        bruker,
                        beregnetTrygdetid.toBeregnetTrygdetid()
                    )
                call.respond(trygdetid.toDto())
            }
        }
    }
}

data class TrygdetidDto(
    val id: UUID,
    val beregnetTrygdetid: BeregnetTrygdetidDto?,
    val trygdetidGrunnlag: List<TrygdetidGrunnlagDto>
)

data class BeregnetTrygdetidDto(
    val nasjonal: Int,
    val fremtidig: Int,
    val total: Int
)

data class TrygdetidGrunnlagDto(
    val id: UUID?,
    val type: String,
    val bosted: String,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val trygdetid: Int,
    val kilde: String
)

fun Trygdetid.toDto(): TrygdetidDto =
    TrygdetidDto(
        id = id,
        beregnetTrygdetid = beregnetTrygdetid?.let {
            BeregnetTrygdetidDto(
                nasjonal = beregnetTrygdetid.nasjonal,
                fremtidig = beregnetTrygdetid.fremtidig,
                total = beregnetTrygdetid.total
            )
        },
        trygdetidGrunnlag = trygdetidGrunnlag.map { it.toDto() }
    )

private fun BeregnetTrygdetidDto.toBeregnetTrygdetid(): BeregnetTrygdetid =
    BeregnetTrygdetid(
        nasjonal = nasjonal,
        fremtidig = fremtidig,
        total = total
    )

private fun TrygdetidGrunnlagDto.toTrygdetidGrunnlag(): TrygdetidGrunnlag =
    TrygdetidGrunnlag(
        id = id ?: UUID.randomUUID(),
        type = TrygdetidType.valueOf(type),
        bosted = bosted,
        periode = TrygdetidPeriode(periodeFra, periodeTil),
        trygdetid = trygdetid,
        kilde = kilde
    )

private fun TrygdetidGrunnlag.toDto(): TrygdetidGrunnlagDto {
    return TrygdetidGrunnlagDto(
        id = id,
        type = type.name,
        bosted = bosted,
        periodeFra = periode.fra,
        periodeTil = periode.til,
        trygdetid = trygdetid,
        kilde = kilde
    )
}