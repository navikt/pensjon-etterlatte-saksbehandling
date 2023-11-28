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
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningkildeDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningsgrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.StatusOppdatertDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidOverstyringDto
import no.nav.etterlatte.libs.common.uuid
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.common.withParam
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Systembruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import java.util.UUID

fun Route.trygdetid(
    trygdetidService: TrygdetidService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/trygdetid/{$BEHANDLINGID_CALL_PARAMETER}") {
        val logger = application.log

        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter trygdetid for behandling $behandlingId")
                val trygdetid = trygdetidService.hentTrygdetid(behandlingId, brukerTokenInfo)
                if (trygdetid != null) {
                    call.respond(trygdetid.toDto())
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        post {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter trygdetid for behandling $behandlingId")
                val trygdetid = trygdetidService.opprettTrygdetid(behandlingId, brukerTokenInfo)
                call.respond(trygdetid.toDto())
            }
        }

        post("overstyr") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppdater trygdetid (overstyring) for behandling $behandlingId")
                val trygdetidOverstyringDto = call.receive<TrygdetidOverstyringDto>()

                val trygdetid =
                    trygdetidService.overstyrNorskPoengaar(
                        trygdetidOverstyringDto.id,
                        behandlingId,
                        trygdetidOverstyringDto.overstyrtNorskPoengaar,
                        brukerTokenInfo,
                    )
                call.respond(trygdetid.toDto())
            }
        }

        post("/grunnlag") {
            withBehandlingId(behandlingKlient) {
                logger.info("Legger til trygdetidsgrunnlag for behandling $behandlingId")
                val trygdetidgrunnlagDto = call.receive<TrygdetidGrunnlagDto>()

                try {
                    trygdetidService.lagreTrygdetidGrunnlag(
                        behandlingId,
                        brukerTokenInfo,
                        trygdetidgrunnlagDto.toTrygdetidGrunnlag(brukerTokenInfo),
                    ).let { trygdetid -> call.respond(trygdetid.toDto()) }
                } catch (overlappendePeriodeException: OverlappendePeriodeException) {
                    logger.info("Klarte ikke legge til ny trygdetidsperiode for $behandlingId pga overlapp.")
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        post("/grunnlag/yrkesskade") {
            withBehandlingId(behandlingKlient) {
                logger.info("Legger til yrkesskade trygdetidsgrunnlag for behandling $behandlingId")
                trygdetidService.lagreYrkesskadeTrygdetidGrunnlag(
                    behandlingId,
                    brukerTokenInfo,
                ).let { trygdetid -> call.respond(trygdetid.toDto()) }
            }
        }

        delete("/grunnlag/{trygdetidGrunnlagId}") {
            withBehandlingId(behandlingKlient) {
                withParam("trygdetidGrunnlagId") { trygdetidGrunnlagId ->
                    logger.info("Sletter trygdetidsgrunnlag for behandling $behandlingId")
                    val trygdetid =
                        trygdetidService.slettTrygdetidGrunnlag(
                            behandlingId,
                            trygdetidGrunnlagId,
                            brukerTokenInfo,
                        )
                    call.respond(trygdetid.toDto())
                }
            }
        }

        post("/kopier/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient) {
                logger.info("Oppretter kopi av forrige trygdetid for behandling $behandlingId")
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")
                trygdetidService.kopierSisteTrygdetidberegning(behandlingId, forrigeBehandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/migrering") {
            post {
                withBehandlingId(behandlingKlient) {
                    logger.info("Migrering overstyrer trygdetid for behandling $behandlingId")

                    val beregnetTrygdetid = call.receive<DetaljertBeregnetTrygdetidResultat>()

                    call.respond(trygdetidService.overstyrBeregnetTrygdetid(behandlingId, beregnetTrygdetid).toDto())
                }
            }

            post("/uten_fremtidig") {
                withBehandlingId(behandlingKlient) {
                    logger.info("Beregn trygdetid uten fremtidig trygdetid for behandling $behandlingId")

                    val trygdetid = trygdetidService.hentTrygdetid(behandlingId, brukerTokenInfo)

                    if (trygdetid != null) {
                        call.respond(
                            trygdetidService.reberegnUtenFremtidigTrygdetid(
                                behandlingId,
                                trygdetid.id,
                                brukerTokenInfo,
                            ).toDto(),
                        )
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }

        post("/oppdater-status") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val statusOppdatert =
                    trygdetidService.sjekkGyldighetOgOppdaterBehandlingStatus(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK, StatusOppdatertDto(statusOppdatert))
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
        opplysninger = this.opplysninger.toDto(),
        overstyrtNorskPoengaar = this.overstyrtNorskPoengaar,
        ident = this.ident,
    )

private fun DetaljertBeregnetTrygdetid.toDto(): DetaljertBeregnetTrygdetidDto =
    DetaljertBeregnetTrygdetidDto(
        resultat = resultat,
        tidspunkt = tidspunkt,
    )

private fun List<Opplysningsgrunnlag>.toDto(): GrunnlagOpplysningerDto =
    GrunnlagOpplysningerDto(
        avdoedFoedselsdato = this.finnOpplysning(TrygdetidOpplysningType.FOEDSELSDATO),
        avdoedDoedsdato = this.finnOpplysning(TrygdetidOpplysningType.DOEDSDATO),
        avdoedFylteSeksten = this.finnOpplysning(TrygdetidOpplysningType.FYLT_16),
        avdoedFyllerSeksti = this.finnOpplysning(TrygdetidOpplysningType.FYLLER_66),
    )

private fun List<Opplysningsgrunnlag>.finnOpplysning(type: TrygdetidOpplysningType): OpplysningsgrunnlagDto? =
    this.find { opplysning -> opplysning.type == type }?.toDto()

private fun Opplysningsgrunnlag.toDto(): OpplysningsgrunnlagDto =
    OpplysningsgrunnlagDto(
        opplysning = this.opplysning,
        kilde =
            when (this.kilde) {
                is Grunnlagsopplysning.Pdl ->
                    OpplysningkildeDto(
                        type = this.kilde.type,
                        tidspunkt = this.kilde.tidspunktForInnhenting.toString(),
                    )

                is Grunnlagsopplysning.RegelKilde ->
                    OpplysningkildeDto(
                        type = this.kilde.type,
                        tidspunkt = this.kilde.ts.toString(),
                    )

                else -> throw Exception("Mangler gyldig kilde for opplysning $id")
            },
    )

fun TrygdetidGrunnlagDto.toTrygdetidGrunnlag(brukerTokenInfo: BrukerTokenInfo): TrygdetidGrunnlag =
    TrygdetidGrunnlag(
        id = id ?: UUID.randomUUID(),
        type = TrygdetidType.valueOf(type),
        bosted = bosted,
        periode = TrygdetidPeriode(periodeFra, periodeTil),
        kilde =
            if (brukerTokenInfo is Systembruker) {
                // p.t. er det kun ved migrering at vi legger til perioder med systembruker
                Grunnlagsopplysning.Pesys(Tidspunkt.now())
            } else {
                Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
            },
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar,
        prorata = prorata,
    )

private fun TrygdetidGrunnlag.toDto(): TrygdetidGrunnlagDto {
    return TrygdetidGrunnlagDto(
        id = id,
        type = type.name,
        bosted = bosted,
        periodeFra = periode.fra,
        periodeTil = periode.til,
        beregnet =
            beregnetTrygdetid?.let {
                BeregnetTrygdetidGrunnlagDto(it.verdi.days, it.verdi.months, it.verdi.years)
            },
        kilde =
            when (kilde) {
                is Grunnlagsopplysning.Saksbehandler ->
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.ident,
                    )

                is Grunnlagsopplysning.Pesys ->
                    TrygdetidGrunnlagKildeDto(
                        tidspunkt = kilde.tidspunkt.toString(),
                        ident = kilde.type,
                    )

                else -> throw UnsupportedOperationException("Kilde for trygdetid maa vaere saksbehandler eller pesys")
            },
        begrunnelse = begrunnelse,
        poengInnAar = poengInnAar,
        poengUtAar = poengUtAar,
        prorata = prorata,
    )
}
