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
import no.nav.etterlatte.libs.common.withParam
import java.time.LocalDate
import java.util.*

fun Route.trygdetid(trygdetidService: TrygdetidService) {
    route("/api/trygdetid/{$BEHANDLINGSID_CALL_PARAMETER}") {
        val logger = application.log

        get {
            // withBehandlingId() TODO
            withParam(BEHANDLINGSID_CALL_PARAMETER) {
                logger.info("Henter trygdetid")
                val trygdetid = trygdetidService.hentTrygdetid(behandlingsId)
                if (trygdetid != null) {
                    call.respond(trygdetid.toDto())
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        post {
            // withBehandlingId() TODO
            withParam(BEHANDLINGSID_CALL_PARAMETER) {
                logger.info("Oppretter trygdetid")
                val trygdetid = trygdetidService.opprettTrygdetid(behandlingsId)
                call.respond(trygdetid.toDto())
            }
        }

        post("/grunnlag") {
            // withBehandlingId() TODO
            withParam(BEHANDLINGSID_CALL_PARAMETER) {
                logger.info("Lagre trygdetidgrunnlag")
                val trygdetidgrunnlagDto = call.receive<TrygdetidGrunnlagDto>()
                val trygdetid = trygdetidService.lagreTrygdetidGrunnlag(behandlingsId, trygdetidgrunnlagDto.fromDto())
                call.respond(trygdetid.toDto())
            }
        }

        post("/oppsummert") {
            // withBehandlingId() TODO
            withParam(BEHANDLINGSID_CALL_PARAMETER) {
                logger.info("Lagre oppsummert trygdetid")
                val oppsummertTrygdetid = call.receive<OppsummertTrygdetidDto>()
                val trygdetid = trygdetidService.lagreOppsummertTrygdetid(behandlingsId, oppsummertTrygdetid.fromDto())
                call.respond(trygdetid.toDto())
            }
        }
    }
}

data class TrygdetidDto(
    val oppsummertTrygdetid: OppsummertTrygdetidDto?,
    val grunnlag: List<TrygdetidGrunnlagDto>
)

fun Trygdetid.toDto(): TrygdetidDto =
    TrygdetidDto(
        oppsummertTrygdetid = oppsummertTrygdetid?.let {
            OppsummertTrygdetidDto(
                nasjonalTrygdetid = oppsummertTrygdetid.nasjonalTrygdetid,
                fremtidigTrygdetid = oppsummertTrygdetid.fremtidigTrygdetid,
                totalt = oppsummertTrygdetid.totalt
            )
        },
        grunnlag = grunnlag.map { it.toDto() }
    )

data class OppsummertTrygdetidDto(
    val nasjonalTrygdetid: Int,
    val fremtidigTrygdetid: Int,
    val totalt: Int
)

fun OppsummertTrygdetidDto.fromDto(): OppsummertTrygdetid =
    OppsummertTrygdetid(
        nasjonalTrygdetid = nasjonalTrygdetid,
        fremtidigTrygdetid = fremtidigTrygdetid,
        totalt = totalt
    )

data class TrygdetidGrunnlagDto(
    val id: UUID?,
    val type: String,
    val bosted: String,
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val kilde: String
)

fun TrygdetidGrunnlagDto.fromDto(): TrygdetidGrunnlag =
    TrygdetidGrunnlag(
        id ?: UUID.randomUUID(),
        TrygdetidType.valueOf(type),
        bosted,
        periodeFra,
        periodeTil,
        kilde
    )

fun TrygdetidGrunnlag.toDto(): TrygdetidGrunnlagDto {
    return TrygdetidGrunnlagDto(
        id,
        type.name,
        bosted,
        periodeFra,
        periodeTil,
        kilde
    )
}