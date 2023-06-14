package no.nav.etterlatte.trygdetid

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningkildeDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningsgrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto
import no.nav.etterlatte.libs.common.uuid
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withParam
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
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

                try {
                    trygdetidService.lagreTrygdetidGrunnlag(
                        behandlingsId,
                        bruker,
                        trygdetidgrunnlagDto.toTrygdetidGrunnlag(bruker)
                    ).let { trygdetid -> call.respond(trygdetid.toDto()) }
                } catch (overlappendePeriodeException: OverlappendePeriodeException) {
                    logger.info("Klarte ikke legge til ny trygdetidsperiode for $behandlingsId pga overlapp.")
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        delete("/grunnlag/{trygdetidGrunnlagId}") {
            withBehandlingId(behandlingKlient) {
                withParam("trygdetidGrunnlagId") { trygdetidGrunnlagId ->
                    logger.info("Sletter trygdetidsgrunnlag for behandling $behandlingsId")
                    val trygdetid = trygdetidService.slettTrygdetidGrunnlag(behandlingsId, trygdetidGrunnlagId, bruker)
                    call.respond(trygdetid.toDto())
                }
            }
        }

        post("/kopier/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter kopi av forrige trygdetid for behandling $behandlingsId")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                trygdetidService.kopierSisteTrygdetidberegning(behandlingsId, forrigeBehandlingId, bruker)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

fun Trygdetid.toDto(): TrygdetidDto =
    TrygdetidDto(
        id = id,
        behandlingId = behandlingId,
        beregnetTrygdetid = beregnetTrygdetid?.toDto(),
        trygdetidGrunnlag = trygdetidGrunnlag.map { it.toDto() },
        opplysninger = this.opplysninger.toDto()
    )

private fun BeregnetTrygdetid.toDto(): BeregnetTrygdetidDto =
    BeregnetTrygdetidDto(
        total = verdi,
        tidspunkt = tidspunkt
    )

private fun List<Opplysningsgrunnlag>.toDto(): GrunnlagOpplysningerDto =
    GrunnlagOpplysningerDto(
        avdoedFoedselsdato = this.finnOpplysning(TrygdetidOpplysningType.FOEDSELSDATO),
        avdoedDoedsdato = this.finnOpplysning(TrygdetidOpplysningType.DOEDSDATO),
        avdoedFylteSeksten = this.finnOpplysning(TrygdetidOpplysningType.FYLT_16),
        avdoedFyllerSeksti = this.finnOpplysning(TrygdetidOpplysningType.FYLLER_66)
    )

private fun List<Opplysningsgrunnlag>.finnOpplysning(type: TrygdetidOpplysningType): OpplysningsgrunnlagDto? =
    this.find { opplysning -> opplysning.type == type }?.toDto()

private fun Opplysningsgrunnlag.toDto(): OpplysningsgrunnlagDto = OpplysningsgrunnlagDto(
    opplysning = this.opplysning,
    kilde = when (this.kilde) {
        is Grunnlagsopplysning.Pdl -> OpplysningkildeDto(
            type = this.kilde.type,
            tidspunkt = this.kilde.tidspunktForInnhenting.toString()
        )

        is Grunnlagsopplysning.RegelKilde -> OpplysningkildeDto(
            type = this.kilde.type,
            tidspunkt = this.kilde.ts.toString()
        )

        else -> throw Exception("Mangler gyldig kilde for opplysning $id")
    }
)

private fun TrygdetidGrunnlagDto.toTrygdetidGrunnlag(bruker: Bruker): TrygdetidGrunnlag =
    TrygdetidGrunnlag(
        id = id ?: UUID.randomUUID(),
        type = TrygdetidType.valueOf(type),
        bosted = bosted,
        periode = TrygdetidPeriode(periodeFra, periodeTil),
        kilde = Grunnlagsopplysning.Saksbehandler(bruker.ident(), Tidspunkt.now()),
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar
    )

private fun TrygdetidGrunnlag.toDto(): TrygdetidGrunnlagDto {
    return TrygdetidGrunnlagDto(
        id = id,
        type = type.name,
        bosted = bosted,
        periodeFra = periode.fra,
        periodeTil = periode.til,
        beregnet = beregnetTrygdetid?.let {
            BeregnetTrygdetidGrunnlagDto(it.verdi.days, it.verdi.months, it.verdi.years)
        },
        kilde = TrygdetidGrunnlagKildeDto(
            tidspunkt = kilde.tidspunkt.toString(),
            ident = kilde.ident
        ),
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar
    )
}