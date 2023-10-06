package no.nav.etterlatte.tilbakekreving.vedtak

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

// TODO Stort sett kopiert modell fra behandling for å sette opp mapping - her kommer det helt sikkert endringer

data class TilbakekrevingVedtak(
    val vedtakId: Long,
    val fattetVedtak: FattetVedtak,
    val perioder: List<TilbakekrevingPeriode>,
    val vurdering: TilbakekrevingVurdering,
    val kontrollfelt: String,
)

data class FattetVedtak(
    val saksbehandler: String,
    val enhet: String,
    val dato: LocalDate,
)

data class TilbakekrevingVurdering(
    val aarsak: TilbakekrevingAarsak,
    val hjemmel: String,
)

data class TilbakekrevingPeriode(
    val maaned: YearMonth,
    val ytelse: Tilbakekrevingsbelop,
    val feilkonto: Tilbakekrevingsbelop,
)

data class Tilbakekrevingsbelop(
    val klasseKode: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val skatteprosent: BigDecimal,
    val beregnetFeilutbetaling: Int,
    val bruttoTilbakekreving: Int,
    val nettoTilbakekreving: Int,
    val skatt: Int,
    val skyld: TilbakekrevingSkyld,
    val resultat: TilbakekrevingResultat,
    val tilbakekrevingsprosent: Int,
    val rentetillegg: Int,
)

enum class TilbakekrevingAarsak {
    ANNET,
    ARBHOYINNT,
    BEREGNFEIL,
    DODSFALL,
    EKTESKAP,
    FEILREGEL,
    FEILUFOREG,
    FLYTTUTLAND,
    IKKESJEKKYTELSE,
    OVERSETTMLD,
    SAMLIV,
    UTBFEILMOT,
}

enum class TilbakekrevingSkyld {
    BRUKER,
    IKKE_FORDELT,
    NAV,
    SKYLDDELING,
}

enum class TilbakekrevingResultat {
    DELVIS_TILBAKEKREV,
    FEILREGISTRERT,
    FORELDET,
    FULL_TILBAKEKREV,
    INGEN_TILBAKEKREV,
}
