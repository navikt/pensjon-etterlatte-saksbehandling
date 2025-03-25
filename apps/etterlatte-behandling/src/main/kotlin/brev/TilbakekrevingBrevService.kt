package no.nav.etterlatte.brev

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.behandling.Soeker
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
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.JaNei
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.kunYtelse
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sak.SakService
import java.util.UUID
import kotlin.math.absoluteValue

class TilbakekrevingBrevService(
    private val sakService: SakService,
    private val brevKlient: BrevKlient,
    private val brevApiKlient: BrevApiKlient,
    private val vedtakKlient: VedtakKlient,
    private val grunnlagService: GrunnlagService,
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

        return brevKlient.opprettStrukturertBrev(
            behandlingId,
            brevRequest,
            bruker,
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

        return brevKlient.genererPdf(brevID, behandlingId, brevRequest, bruker)
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

        return brevKlient.tilbakestillStrukturertBrev(
            brevID,
            behandlingId,
            brevRequest,
            bruker,
        )
    }

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        brevKlient.ferdigstillStrukturertBrev(behandlingId, Brevtype.VEDTAK, brukerTokenInfo)
    }

    suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? = brevApiKlient.hentVedtaksbrev(behandlingId, bruker)

    private suspend fun utledBrevRequest(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
        skalLagres: Boolean = false,
    ): BrevRequest =
        coroutineScope {
            val sak = sakService.finnSak(sakId) ?: throw InternfeilException("Fant ikke sak med id=$sakId")

            val vedtakDeferred =
                async {
                    vedtakKlient.hentVedtak(behandlingId, bruker)
                }
            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)
                    ?: throw InternfeilException("Fant ikke grunnlag med sakId=$sakId")

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

        // Identifisere trekk som brukes for å trekke 17% skatt på barnepensjon. Dette legges til for at brev skal
        // bli riktig i tilfeller hvor skatt ikke har blitt overført til skatteetaten.
        val fastSkattetrekkBarnepensjon =
            periode.tilbakekrevingsbeloep
                .filter { it.klasseType == KlasseType.TREK.name && it.klasseKode == "BPSKSKAT" }
                .sumOf { beloep -> beloep.bruttoUtbetaling.absoluteValue }

        if (fastSkattetrekkBarnepensjon > 0) {
            logger.info(
                "Identifisert skattetrekk på $fastSkattetrekkBarnepensjon i ${tilbakekreving.kravgrunnlag.sakId.value}. Brevet justeres basert på dette.",
            )
        }

        TilbakekrevingPeriodeDataNy(
            maaned = periode.maaned.atDay(1),
            beloeper =
                periode.let { beloeper ->
                    val beregnetFeilutbetaling =
                        beloepMedKunYtelse.sumOf { beloep ->
                            beloep.beregnetFeilutbetaling ?: 0
                        } + fastSkattetrekkBarnepensjon
                    val bruttoTilbakekreving =
                        beloepMedKunYtelse.sumOf { beloep ->
                            beloep.bruttoTilbakekreving ?: 0
                        } + fastSkattetrekkBarnepensjon
                    val skatt =
                        beloepMedKunYtelse.sumOf { beloep -> beloep.skatt ?: 0 } + fastSkattetrekkBarnepensjon
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
