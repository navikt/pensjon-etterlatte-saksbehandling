package no.nav.etterlatte.brev.model.tilbakekreving

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAktsomhet
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class TilbakekrevingFerdigData(
    val innhold: List<Slate.Element>,
    val data: TilbakekrevingInnholdBrevData,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            innholdMedVedlegg: InnholdMedVedlegg,
        ) = TilbakekrevingFerdigData(
            innhold = innholdMedVedlegg.innhold(),
            data = TilbakekrevingInnholdBrevData.fra(generellBrevData),
        )
    }
}

data class TilbakekrevingInnholdBrevData(
    val sakType: SakType,
    val harRenter: Boolean,
    val harStrafferettslig: Boolean,
    val harForeldelse: Boolean,
    val perioder: List<TilbakekrevingPeriodeData>,
    val summer: TilbakekrevingBeloeperData,
) : BrevData() {
    companion object {
        fun fra(generellBrevData: GenerellBrevData): TilbakekrevingInnholdBrevData {
            val tilbakekreving =
                generellBrevData.forenkletVedtak?.tilbakekreving
                    ?: throw BrevDataTilbakerevingHarManglerException("Vedtak mangler tilbakekreving")
            return TilbakekrevingInnholdBrevData(
                sakType = generellBrevData.sak.sakType,
                harRenter = sjekkOmHarRenter(tilbakekreving),
                harStrafferettslig = tilbakekreving.vurdering.aktsomhet.aktsomhet == TilbakekrevingAktsomhet.GROV_UAKTSOMHET,
                harForeldelse = sjekkOmHarForeldet(tilbakekreving),
                perioder = tilbakekrevingsPerioder(tilbakekreving),
                summer = perioderSummert(tilbakekreving),
            )
        }

        private fun sjekkOmHarRenter(tilbakekreving: Tilbakekreving) =
            tilbakekreving.perioder.any { periode ->
                periode.ytelse.rentetillegg.let { it != null && it > 0 }
            }

        private fun sjekkOmHarForeldet(tilbakekreving: Tilbakekreving) =
            tilbakekreving.perioder.any { periode ->
                periode.ytelse.resultat.let { it != null && it == TilbakekrevingResultat.FORELDET }
            }

        private fun tilbakekrevingsPerioder(tilbakekreving: Tilbakekreving) =
            tilbakekreving.perioder.map { periode ->
                TilbakekrevingPeriodeData(
                    maaned = periode.maaned.atDay(1),
                    beloeper =
                        periode.ytelse.let { beloeper ->
                            TilbakekrevingBeloeperData(
                                feilutbetaling = Kroner(beloeper.beregnetFeilutbetaling ?: 0),
                                bruttoTilbakekreving = Kroner(beloeper.bruttoTilbakekreving ?: 0),
                                fradragSkatt = Kroner(beloeper.skatt ?: 0),
                                nettoTilbakekreving = Kroner(beloeper.nettoTilbakekreving ?: 0),
                                renteTillegg = Kroner(beloeper.rentetillegg ?: 0),
                            )
                        },
                    resultat =
                        periode.ytelse.resultat?.name
                            ?: throw BrevDataTilbakerevingHarManglerException("Alle perioder må ha resultat"),
                )
            }

        private fun perioderSummert(tilbakekreving: Tilbakekreving) =
            TilbakekrevingBeloeperData(
                feilutbetaling = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.beregnetFeilutbetaling ?: 0 }),
                bruttoTilbakekreving = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.bruttoTilbakekreving ?: 0 }),
                fradragSkatt = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.skatt ?: 0 }),
                nettoTilbakekreving = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.nettoTilbakekreving ?: 0 }),
                renteTillegg = Kroner(tilbakekreving.perioder.sumOf { it.ytelse.rentetillegg ?: 0 }),
            )
    }
}

data class TilbakekrevingPeriodeData(
    val maaned: LocalDate,
    val beloeper: TilbakekrevingBeloeperData,
    val resultat: String,
)

data class TilbakekrevingBeloeperData(
    val feilutbetaling: Kroner,
    val bruttoTilbakekreving: Kroner,
    val nettoTilbakekreving: Kroner,
    val fradragSkatt: Kroner,
    val renteTillegg: Kroner,
)

class BrevDataTilbakerevingHarManglerException(message: String) : RuntimeException(message)
