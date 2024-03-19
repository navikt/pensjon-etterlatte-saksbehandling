package no.nav.etterlatte.brev.model.tilbakekreving

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.formaterNavn
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class TilbakekrevingBrevDTO(
    override val innhold: List<Slate.Element>,
    val sakType: SakType,
    val bosattUtland: Boolean,
    val brukerNavn: String,
    val doedsbo: Boolean,
    val varselVedlagt: Boolean,
    val datoVarselEllerVedtak: LocalDate,
    val datoTilsvarBruker: LocalDate?,
    val tilbakekreving: TilbakekrevingData,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            redigerbart: List<Slate.Element>,
        ): TilbakekrevingBrevDTO {
            val tilbakekreving =
                generellBrevData.forenkletVedtak?.tilbakekreving
                    ?: throw BrevDataTilbakerevingHarManglerException("Vedtak mangler tilbakekreving")
            return TilbakekrevingBrevDTO(
                innhold = redigerbart,
                sakType = generellBrevData.sak.sakType,
                bosattUtland = generellBrevData.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                brukerNavn = generellBrevData.personerISak.soeker.formaterNavn(),
                // TODO hvordan vet vi om det er dødsbo?
                doedsbo = false,
                // TODO varselVedlagt Hvordn vet vi det?
                varselVedlagt = false,
                // TODO hvis varsel ikke er vedlagt skal varsel sin dato brukes..
                datoVarselEllerVedtak = generellBrevData.forenkletVedtak.vedtaksdato ?: LocalDate.now(),
                // TODO hvordan vet vi tilsvar bruker?
                datoTilsvarBruker = null,
                tilbakekreving =
                    TilbakekrevingData(
                        fraOgMed = tilbakekreving.perioder.first().maaned.atDay(1),
                        tilOgMed = tilbakekreving.perioder.last().maaned.atEndOfMonth(),
                        skalTilbakekreve =
                            tilbakekreving.perioder.any {
                                it.ytelse.resultat == TilbakekrevingResultat.FULL_TILBAKEKREV ||
                                    it.ytelse.resultat == TilbakekrevingResultat.DELVIS_TILBAKEKREV
                            },
                        helTilbakekreving =
                            tilbakekreving.perioder.all {
                                it.ytelse.resultat == TilbakekrevingResultat.FULL_TILBAKEKREV
                            },
                        perioder = tilbakekrevingsPerioder(tilbakekreving),
                        summer = perioderSummert(tilbakekreving),
                    ),
            )
        }

        private fun tilbakekrevingsPerioder(tilbakekreving: Tilbakekreving) =
            tilbakekreving.perioder.map { periode ->
                TilbakekrevingPeriodeData(
                    maaned = periode.maaned.atDay(1),
                    beloeper =
                        periode.ytelse.let { beloeper ->
                            val netto = tilbakekreving.perioder.sumOf { it.ytelse.nettoTilbakekreving ?: 0 }
                            val renteTillegg = tilbakekreving.perioder.sumOf { it.ytelse.rentetillegg ?: 0 }
                            TilbakekrevingBeloeperData(
                                feilutbetaling = Kroner(beloeper.beregnetFeilutbetaling ?: 0),
                                bruttoTilbakekreving = Kroner(beloeper.bruttoTilbakekreving ?: 0),
                                fradragSkatt = Kroner(beloeper.skatt ?: 0),
                                nettoTilbakekreving = Kroner(netto),
                                renteTillegg = Kroner(renteTillegg),
                                sumNettoRenter = Kroner(netto + renteTillegg),
                            )
                        },
                    resultat =
                        periode.ytelse.resultat
                            ?: throw BrevDataTilbakerevingHarManglerException("Alle perioder må ha resultat"),
                )
            }

        private fun perioderSummert(tilbakekreving: Tilbakekreving): TilbakekrevingBeloeperData {
            val netto = tilbakekreving.perioder.sumOf { it.ytelse.nettoTilbakekreving ?: 0 }
            val renteTillegg = tilbakekreving.perioder.sumOf { it.ytelse.rentetillegg ?: 0 }
            return TilbakekrevingBeloeperData(
                feilutbetaling = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.beregnetFeilutbetaling ?: 0 }),
                bruttoTilbakekreving = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.bruttoTilbakekreving ?: 0 }),
                fradragSkatt = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.skatt ?: 0 }),
                nettoTilbakekreving = Kroner(netto),
                renteTillegg = Kroner(renteTillegg),
                sumNettoRenter = Kroner(netto + renteTillegg),
            )
        }
    }
}

data class TilbakekrevingData(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val skalTilbakekreve: Boolean,
    val helTilbakekreving: Boolean,
    val perioder: List<TilbakekrevingPeriodeData>,
    val summer: TilbakekrevingBeloeperData,
)

data class TilbakekrevingPeriodeData(
    val maaned: LocalDate,
    val beloeper: TilbakekrevingBeloeperData,
    val resultat: TilbakekrevingResultat,
)

data class TilbakekrevingBeloeperData(
    val feilutbetaling: Kroner,
    val bruttoTilbakekreving: Kroner,
    val nettoTilbakekreving: Kroner,
    val fradragSkatt: Kroner,
    val renteTillegg: Kroner,
    val sumNettoRenter: Kroner,
)

class BrevDataTilbakerevingHarManglerException(message: String) : RuntimeException(message)
