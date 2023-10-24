package no.nav.etterlatte.trygdetid

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.MigreringOverstyringDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidOverstyringDto
import no.nav.etterlatte.libs.common.uuid
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withParam
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private const val TRYGDETIDID_CALL_PARAMETER = "trygdetidId"

fun PipelineContext<*, ApplicationCall>.trygdetidId(): UUID {
    return try {
        this.call.parameters[TRYGDETIDID_CALL_PARAMETER]?.let { UUID.fromString(it) }!!
    } catch (e: Exception) {
        throw UgyldigForespoerselException("MANGLER_TRYGDETID_ID", "Kunne ikke lese ut parameteret trygdetidId")
    }
}

private val logger: Logger = LoggerFactory.getLogger("TrygdetidV2Route")

fun Route.trygdetidV2(
    trygdetidService: NyTrygdetidService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/trygdetid_v2/{$BEHANDLINGSID_CALL_PARAMETER}") {
        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter trygdetid for behandling $behandlingsId")
                val trygdetider = trygdetidService.hentTrygdetiderIBehandling(behandlingsId, brukerTokenInfo)
                if (trygdetider.isNotEmpty()) {
                    call.respond(trygdetider.map { it.toDto() })
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        post {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter trygdetid(er) for behandling $behandlingsId")
                val trygdetid = trygdetidService.opprettTrygdetiderForBehandling(behandlingsId, brukerTokenInfo)
                call.respond(trygdetid.map { it.toDto() })
            }
        }

        post("overstyr") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppdater trygdetid (overstyring) for behandling $behandlingsId")
                val trygdetidOverstyringDto = call.receive<TrygdetidOverstyringDto>()

                val trygdetid =
                    trygdetidService.overstyrNorskPoengaaarForTrygdetid(
                        trygdetidOverstyringDto.id,
                        behandlingsId,
                        trygdetidOverstyringDto.overstyrtNorskPoengaar,
                    )
                call.respond(trygdetid.toDto())
            }
        }

        route("{${TRYGDETIDID_CALL_PARAMETER}}") {
            post("grunnlag") {
                withBehandlingId(behandlingKlient) {
                    logger.info("Legger til trygdetidsgrunnlag for behandling $behandlingsId")
                    val trygdetidgrunnlagDto = call.receive<TrygdetidGrunnlagDto>()

                    trygdetidService.lagreTrygdetidGrunnlagForTrygdetidMedIdIBehandling(
                        behandlingsId,
                        trygdetidId(),
                        trygdetidgrunnlagDto.toTrygdetidGrunnlag(brukerTokenInfo),
                        brukerTokenInfo,
                    ).let { trygdetid -> call.respond(trygdetid.toDto()) }
                }
            }

            post("/grunnlag/yrkesskade") {
                withBehandlingId(behandlingKlient) {
                    logger.info("Legger til yrkesskade trygdetidsgrunnlag for behandling $behandlingsId")
                    trygdetidService.lagreYrkesskadeTrygdetidGrunnlag(
                        behandlingsId,
                        brukerTokenInfo,
                    ).let { trygdetid -> call.respond(trygdetid.toDto()) }
                }
            }

            delete("/grunnlag/{trygdetidGrunnlagId}") {
                withBehandlingId(behandlingKlient) {
                    withParam("trygdetidGrunnlagId") { trygdetidGrunnlagId ->
                        logger.info("Sletter trygdetidsgrunnlag for behandling $behandlingsId")
                        val trygdetid =
                            trygdetidService.slettTrygdetidGrunnlagForTrygdetid(
                                behandlingsId,
                                trygdetidId(),
                                trygdetidGrunnlagId,
                                brukerTokenInfo,
                            )
                        call.respond(trygdetid.toDto())
                    }
                }
            }
        }

        post("/kopier/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter kopi av forrige trygdetider for behandling $behandlingsId")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                trygdetidService.kopierSisteTrygdetidberegninger(behandlingsId, forrigeBehandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/migrering") {
            withBehandlingId(behandlingKlient) {
                logger.info("Migrering overstyrer trygdetid for behandling $behandlingsId")

                val overstyringDto = call.receive<MigreringOverstyringDto>()

                call.respond(
                    trygdetidService.overstyrBeregnetTrygdetidForAvdoed(
                        behandlingsId,
                        overstyringDto.ident,
                        overstyringDto.detaljertBeregnetTrygdetidResultat,
                    ).toDto(),
                )
            }
        }
    }
}
