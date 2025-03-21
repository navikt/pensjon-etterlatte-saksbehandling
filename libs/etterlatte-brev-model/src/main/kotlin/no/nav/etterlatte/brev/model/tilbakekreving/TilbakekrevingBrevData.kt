package no.nav.etterlatte.brev.model.tilbakekreving

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.brev.BrevInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVarsel
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

@JsonTypeName("TILBAKEKREVING")
data class TilbakekrevingBrevInnholdDataNy(
    val sakType: SakType,
    val bosattUtland: Boolean,
    val brukerNavn: String,
    val doedsbo: Boolean,
    val varsel: TilbakekrevingVarsel,
    val datoVarselEllerVedtak: LocalDate,
    val datoTilsvarBruker: LocalDate?,
    val tilbakekreving: TilbakekrevingDataNy,
) : BrevInnholdData() {
    override val type: String = "TILBAKEKREVING"
    override val brevKode: Brevkoder = Brevkoder.TILBAKEKREVING
}

data class TilbakekrevingDataNy(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val skalTilbakekreve: Boolean,
    val helTilbakekreving: Boolean,
    val harRenteTillegg: Boolean,
    val perioder: List<TilbakekrevingPeriodeDataNy>,
    val summer: TilbakekrevingBeloeperDataNy,
)

data class TilbakekrevingPeriodeDataNy(
    val maaned: LocalDate,
    val beloeper: TilbakekrevingBeloeperDataNy,
    val resultat: TilbakekrevingResultat,
)

data class TilbakekrevingBeloeperDataNy(
    val feilutbetaling: Kroner,
    val bruttoTilbakekreving: Kroner,
    val nettoTilbakekreving: Kroner,
    val fradragSkatt: Kroner,
    val renteTillegg: Kroner,
    val sumNettoRenter: Kroner,
) {
    /*
    Hensikten med disse metodene er for å slippe å legge til brevbaker-api-model i behandling
     */
    fun feilutbetaling() = feilutbetaling.value

    fun bruttoTilbakekreving() = bruttoTilbakekreving.value

    fun nettoTilbakekreving() = nettoTilbakekreving.value

    fun fradragSkatt() = fradragSkatt.value

    fun renteTillegg() = renteTillegg.value

    companion object {
        fun opprett(
            feilutbetaling: Int,
            bruttoTilbakekreving: Int,
            nettoTilbakekreving: Int,
            fradragSkatt: Int,
            renteTillegg: Int,
            sumNettoRenter: Int,
        ) = TilbakekrevingBeloeperDataNy(
            feilutbetaling = Kroner(feilutbetaling),
            bruttoTilbakekreving = Kroner(bruttoTilbakekreving),
            nettoTilbakekreving = Kroner(nettoTilbakekreving),
            fradragSkatt = Kroner(fradragSkatt),
            renteTillegg = Kroner(renteTillegg),
            sumNettoRenter = Kroner(sumNettoRenter),
        )
    }
}
