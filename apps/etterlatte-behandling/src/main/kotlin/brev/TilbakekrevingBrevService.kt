package no.nav.etterlatte.brev

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.erOver18
import no.nav.etterlatte.brev.behandling.hentForelderVerge
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBeloeperDataNy
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBrevInnholdDataNy
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingDataNy
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingPeriodeDataNy
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentVerger
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.JaNei
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.kunYtelse
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.sikkerLogg
import java.util.UUID

class TilbakekrevingBrevService(
    val sakService: SakService,
    val brevKlient: BrevKlient,
    val vedtakKlient: VedtakKlient,
    val grunnlagService: GrunnlagService,
) {
    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Brev {
        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(bruker, behandlingId, sakId)
            }

        return brevKlient.opprettVedtaksbrev(
            behandlingId,
            bruker,
            brevRequest,
        )
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
        skalLagres: Boolean,
    ): Pdf {
        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(bruker, behandlingId, sakId, skalLagres)
            }

        return brevKlient.genererPdf(brevID, behandlingId, bruker, brevRequest)
    }

    suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): BrevPayload {
        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(bruker, behandlingId, sakId)
            }

        return brevKlient.tilbakestillVedtaksbrev(
            brevID,
            behandlingId,
            bruker,
            brevRequest,
        )
    }

    private suspend fun utledBrevRequest(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
        skalLagres: Boolean = false,
    ): BrevRequest =
        coroutineScope {
            val sak =
                inTransaction {
                    sakService.finnSak(sakId) ?: throw InternfeilException("Fant ikke sak med id=$sakId")
                }

            val vedtakDeferred =
                async {
                    vedtakKlient.hentVedtak(behandlingId, bruker)
                }
            val grunnlagDef =
                async {
                    grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)!!
                }

            // TODO trenger vi egt async?
            val grunnlag = grunnlagDef.await()

            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Kan ikke lage vedtaksbrev for tilbakekreving uten vedtak behandlingId=$behandlingId")

            val verge = hentVergeForSak(sak.sakType, null, grunnlag)
            val soeker = grunnlag.mapSoeker(null)

            val innloggetSaksbehandlerIdent = bruker.ident() // TODO bør ikke være nødvendig for kun vedtaksbrev?
            val saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
            val attestantIdent = vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent

            BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = soeker,
                avdoede = grunnlag.mapAvdoede(),
                verge = verge,
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = saksbehandlerIdent,
                attestantIdent = attestantIdent,
                skalLagre = skalLagres,
                // TODO kun nødvendig ved foråhdsvisning/ferdigstilling?
                brevInnholdData =
                    utledBrevInnholdData(
                        sakType = sak.sakType,
                        soeker = soeker,
                        vedtak = vedtak,
                    ),
            )
        }

    // TODO trekkes ut som felles metode
    private fun hentVergeForSak(
        sakType: SakType,
        brevutfallDto: BrevutfallDto?,
        grunnlag: Grunnlag,
    ): Verge? {
        val soekerPdl =
            grunnlag.soeker.hentSoekerPdlV1()
                ?: throw InternfeilException(
                    "Finner ikke søker i grunnlaget. Dette kan komme av flere ting, bl.a. endret ident på bruker. " +
                        "Hvis dette ikke er tilfellet må feilen meldes i Porten.",
                )

        val verger =
            hentVerger(
                soekerPdl.verdi.vergemaalEllerFremtidsfullmakt ?: emptyList(),
                grunnlag.soeker.hentFoedselsnummer()?.verdi,
            )
        return if (verger.size == 1) {
            val vergeFnr = verger.first().vergeEllerFullmektig.motpartsPersonident
            if (vergeFnr == null) {
                logger.error(
                    "Vi genererer et brev til en person som har verge uten ident. Det er verdt å følge " +
                        "opp saken ekstra, for å sikre at det ikke blir noe feil her (koble på fag). saken har " +
                        "id=${grunnlag.metadata.sakId}. Denne loggmeldingen kan nok fjernes etter at løpet her" +
                        " er kvalitetssikret.",
                )
                UkjentVergemaal()
            } else {
                // TODO: Hente navn direkte fra Grunnlag eller PDL
                val vergenavn = "placeholder for vergenavn"

                Vergemaal(
                    vergenavn,
                    vergeFnr,
                )
            }
        } else if (verger.size > 1) {
            logger.info(
                "Fant flere verger for bruker med fnr ${grunnlag.soeker.hentFoedselsnummer()?.verdi} i " +
                    "mapping av verge til brev.",
            )
            sikkerLogg.info(
                "Fant flere verger for bruker med fnr ${
                    grunnlag.soeker.hentFoedselsnummer()?.verdi?.value
                } i mapping av verge til brev.",
            )
            UkjentVergemaal()
        } else if (sakType == SakType.BARNEPENSJON && !grunnlag.erOver18(brevutfallDto?.aldersgruppe)) {
            grunnlag.hentForelderVerge()
        } else {
            null
        }
    }
}

fun Soeker.formaterNavn() = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")

fun utledBrevInnholdData(
    sakType: SakType,
    soeker: Soeker,
    vedtak: VedtakDto,
): TilbakekrevingBrevInnholdDataNy {
    val tilbakekreving =
        objectMapper.readValue<Tilbakekreving>(
            (vedtak.innhold as VedtakInnholdDto.VedtakTilbakekrevingDto).tilbakekreving.toJson(),
        )

    val perioderSortert = tilbakekreving.perioder.sortedBy { it.maaned }

    return TilbakekrevingBrevInnholdDataNy(
        sakType = sakType,
        bosattUtland = false, // TODO hvordan finne bosatt utland for tilbakekreving?
        brukerNavn = soeker.formaterNavn(),
        doedsbo = tilbakekreving.vurdering?.doedsbosak == JaNei.JA,
        varsel = tilbakekreving.vurdering?.forhaandsvarsel ?: throw TilbakeKrevingManglerVarsel(),
        datoVarselEllerVedtak =
            tilbakekreving.vurdering?.forhaandsvarselDato
                ?: throw TilbakeKrevingManglerForhaandsvarselDatoException(),
        datoTilsvarBruker = tilbakekreving.vurdering?.tilsvar?.dato,
        tilbakekreving =
            TilbakekrevingDataNy(
                fraOgMed = perioderSortert.first().maaned.atDay(1),
                tilOgMed = perioderSortert.last().maaned.atEndOfMonth(),
                skalTilbakekreve =
                    tilbakekreving.perioder.any {
                        it.tilbakekrevingsbeloep.kunYtelse().any { beloep ->
                            beloep.resultat == TilbakekrevingResultat.FULL_TILBAKEKREV ||
                                beloep.resultat == TilbakekrevingResultat.DELVIS_TILBAKEKREV
                        }
                    },
                helTilbakekreving =
                    tilbakekreving.perioder.any {
                        it.tilbakekrevingsbeloep.kunYtelse().any { beloep ->
                            beloep.resultat == TilbakekrevingResultat.FULL_TILBAKEKREV
                        }
                    },
                perioder = tilbakekrevingsPerioder(tilbakekreving),
                harRenteTillegg = sjekkOmHarRenter(tilbakekreving),
                summer = perioderSummert(tilbakekreving),
            ),
    )
}

private fun tilbakekrevingsPerioder(tilbakekreving: Tilbakekreving) =
    tilbakekreving.perioder.map { periode ->
        val beloepMedKunYtelse = periode.tilbakekrevingsbeloep.kunYtelse()

        TilbakekrevingPeriodeDataNy(
            maaned = periode.maaned.atDay(1),
            beloeper =
                periode.let { beloeper ->
                    val beregnetFeilutbetaling =
                        beloepMedKunYtelse.sumOf { beloep -> beloep.beregnetFeilutbetaling ?: 0 }
                    val bruttoTilbakekreving =
                        beloepMedKunYtelse.sumOf { beloep -> beloep.bruttoTilbakekreving ?: 0 }
                    val skatt = beloepMedKunYtelse.sumOf { beloep -> beloep.skatt ?: 0 }
                    val netto = beloepMedKunYtelse.sumOf { beloep -> beloep.nettoTilbakekreving ?: 0 }
                    val renteTillegg = beloepMedKunYtelse.sumOf { beloep -> beloep.rentetillegg ?: 0 }

                    TilbakekrevingBeloeperDataNy.opprett(
                        feilutbetaling = beregnetFeilutbetaling,
                        bruttoTilbakekreving = bruttoTilbakekreving,
                        fradragSkatt = skatt,
                        nettoTilbakekreving = netto,
                        renteTillegg = renteTillegg,
                        sumNettoRenter = netto + renteTillegg,
                    )
                },
            resultat =
                beloepMedKunYtelse
                    .map {
                        it.resultat ?: throw TilbakekrevingManglerResultatException("Alle perioder må ha resultat")
                    }.let {
                        TilbakekrevingResultat.hoyesteGradAvTilbakekreving(it)
                    } ?: throw TilbakekrevingManglerResultatException("Fant ingen resultat"),
        )
    }

private fun perioderSummert(tilbakekreving: Tilbakekreving): TilbakekrevingBeloeperDataNy {
    val perioder = tilbakekrevingsPerioder(tilbakekreving)

    val netto = perioder.sumOf { it.beloeper.nettoTilbakekreving() }
    val renteTillegg = perioder.sumOf { it.beloeper.renteTillegg() }
    return TilbakekrevingBeloeperDataNy.opprett(
        feilutbetaling = perioder.sumOf { it.beloeper.feilutbetaling() },
        bruttoTilbakekreving = perioder.sumOf { it.beloeper.bruttoTilbakekreving() },
        fradragSkatt = perioder.sumOf { it.beloeper.fradragSkatt() },
        nettoTilbakekreving = netto,
        renteTillegg = renteTillegg,
        sumNettoRenter = netto + renteTillegg,
    )
}

private fun sjekkOmHarRenter(tilbakekreving: Tilbakekreving) =
    tilbakekreving.perioder.any { periode ->
        periode.tilbakekrevingsbeloep.any {
            it.rentetillegg?.let { rentetillegg -> rentetillegg > 0 }
                ?: false
        }
    }

class TilbakekrevingManglerResultatException(
    message: String,
) : UgyldigForespoerselException(
        code = "TILBAKEKREVING_MANGLER_RESULTAT",
        detail = message,
    )

class TilbakeKrevingManglerForhaandsvarselDatoException :
    UgyldigForespoerselException(
        code = "TILBAKEKREVING_MANGLER_VURDERING_FORHÅNDSVARSELSDATO",
        detail = "Kan ikke generere pdf uten vurdering forhaandsvarselDato av tilbakekreving",
    )

class TilbakeKrevingManglerVarsel :
    UgyldigForespoerselException(
        code = "TILBAKEKREVING_MANGLER_VURDERING_VARSEL",
        detail = "Kan ikke generere pdf uten at varsel er satt under vurdering",
    )
