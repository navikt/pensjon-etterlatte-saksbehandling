package no.nav.etterlatte.brev.tilbakekreving

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.BrevInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.brevbaker.formaterNavn
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.hentinformasjon.behandling.BehandlingService
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.hentinformasjon.vedtaksvurdering.VedtaksvurderingService
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakeKrevingManglerForhaandsvarselDatoException
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakeKrevingManglerVarsel
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingBeloeperData
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingData
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingManglerResultatException
import no.nav.etterlatte.brev.model.tilbakekreving.TilbakekrevingPeriodeData
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.JaNei
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVarsel
import no.nav.etterlatte.libs.common.tilbakekreving.kunYtelse
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.util.UUID

class SkalTilBehandling(
    val service: TilbakekrevingVedtaksbrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
    private val grunnlagService: GrunnlagService,
    private val behandlingService: BehandlingService,
    private val db: BrevRepository,
) {
    suspend fun opprettVedtaksbrev(
        sakId: SakId,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(bruker, behandlingId, sakId)
            }

        service.opprettVedtaksbrev(
            behandlingId,
            bruker,
            brevRequest,
        )

        // service.genererPdf()
    }

    suspend fun genererPdf(
        brevID: BrevID,
        bruker: BrukerTokenInfo,
    ): Pdf {
        val brev = db.hentBrev(brevID)

        val brevRequest =
            retryOgPakkUt {
                utledBrevRequest(bruker, brev.behandlingId!!, brev.sakId)
            }

        return service.genererPdf(brevID, bruker, brevRequest)
    }

    // TODO skal utledes i behandling som kaller brev-api
    private suspend fun utledBrevRequest(
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
    ): BrevRequest =
        coroutineScope {
            val sak =
                async {
                    behandlingService.hentSak(sakId, bruker)
                }
            val vedtakDeferred =
                async {
                    vedtaksvurderingService.hentVedtak(behandlingId, bruker)
                }
            val grunnlag =
                async {
                    grunnlagService.hentGrunnlag(VedtakType.TILBAKEKREVING, sakId, bruker, behandlingId)
                }
            val verge =
                async {
                    grunnlagService.hentVergeForSak(sak.await().sakType, null, grunnlag.await())
                }

            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Kan ikke lage vedtaksbrev for tilbakekreving uten vedtak behandlingId=$behandlingId")

            val personerISak =
                grunnlag.await().let {
                    PersonerISak(
                        innsender = it.mapInnsender(),
                        soeker = it.mapSoeker(null),
                        avdoede = it.mapAvdoede(),
                        verge = verge.await(),
                    )
                }

            BrevRequest(
                sak = sak.await(),
                personerISak = personerISak,
                vedtak = vedtak,
                spraak = grunnlag.await().mapSpraak(),
                // TODO Trengs kun ved foråhdsvisning/ferdigstilling..
                brevInnholdData =
                    TilbakekrevingBrevInnholdData.fra(
                        sakType = sak.await().sakType,
                        personerISak = personerISak,
                        vedtak = vedtak,
                    ),
            )
        }
}

// TODO på tide men en lib for disse? litt mas med dupklater på tvers?
data class TilbakekrevingBrevInnholdData(
    override val brevKode: Brevkoder = Brevkoder.TILBAKEKREVING,
    val sakType: SakType,
    val bosattUtland: Boolean,
    val brukerNavn: String,
    val doedsbo: Boolean,
    val varsel: TilbakekrevingVarsel,
    val datoVarselEllerVedtak: LocalDate,
    val datoTilsvarBruker: LocalDate?,
    val tilbakekreving: TilbakekrevingData,
) : BrevInnholdData {
    companion object {
        fun fra(
            sakType: SakType,
            personerISak: PersonerISak,
            vedtak: VedtakDto,
        ): TilbakekrevingBrevInnholdData {
            val tilbakekreving =
                objectMapper.readValue<Tilbakekreving>(
                    (vedtak.innhold as VedtakInnholdDto.VedtakTilbakekrevingDto).tilbakekreving.toJson(),
                )

            val perioderSortert = tilbakekreving.perioder.sortedBy { it.maaned }

            return TilbakekrevingBrevInnholdData(
                sakType = sakType,
                bosattUtland = false, // TODO må hente behandling...
                brukerNavn = personerISak.soeker.formaterNavn(),
                doedsbo = tilbakekreving.vurdering?.doedsbosak == JaNei.JA,
                varsel = tilbakekreving.vurdering?.forhaandsvarsel ?: throw TilbakeKrevingManglerVarsel(),
                datoVarselEllerVedtak =
                    tilbakekreving.vurdering?.forhaandsvarselDato
                        ?: throw TilbakeKrevingManglerForhaandsvarselDatoException(),
                datoTilsvarBruker = tilbakekreving.vurdering?.tilsvar?.dato,
                tilbakekreving =
                    TilbakekrevingData(
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

                TilbakekrevingPeriodeData(
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

                            TilbakekrevingBeloeperData(
                                feilutbetaling = Kroner(beregnetFeilutbetaling),
                                bruttoTilbakekreving = Kroner(bruttoTilbakekreving),
                                fradragSkatt = Kroner(skatt),
                                nettoTilbakekreving = Kroner(netto),
                                renteTillegg = Kroner(renteTillegg),
                                sumNettoRenter = Kroner(netto + renteTillegg),
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

        private fun perioderSummert(tilbakekreving: Tilbakekreving): TilbakekrevingBeloeperData {
            val perioder = tilbakekrevingsPerioder(tilbakekreving)

            val netto = perioder.sumOf { it.beloeper.nettoTilbakekreving.value }
            val renteTillegg = perioder.sumOf { it.beloeper.renteTillegg.value }
            return TilbakekrevingBeloeperData(
                feilutbetaling = Kroner(perioder.sumOf { it.beloeper.feilutbetaling.value }),
                bruttoTilbakekreving = Kroner(perioder.sumOf { it.beloeper.bruttoTilbakekreving.value }),
                fradragSkatt = Kroner(perioder.sumOf { it.beloeper.fradragSkatt.value }),
                nettoTilbakekreving = Kroner(netto),
                renteTillegg = Kroner(renteTillegg),
                sumNettoRenter = Kroner(netto + renteTillegg),
            )
        }

        private fun sjekkOmHarRenter(tilbakekreving: Tilbakekreving) =
            tilbakekreving.perioder.any { periode ->
                periode.tilbakekrevingsbeloep.any {
                    it.rentetillegg?.let { rentetillegg -> rentetillegg > 0 }
                        ?: false
                }
            }
    }
}
