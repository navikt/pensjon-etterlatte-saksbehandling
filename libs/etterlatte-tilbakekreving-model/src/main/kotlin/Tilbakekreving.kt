package no.nav.etterlatte.libs.common.tilbakekreving

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.sak.SakId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Tilbakekreving(
    val vurdering: TilbakekrevingVurdering?,
    val perioder: List<TilbakekrevingPeriode>,
    val kravgrunnlag: Kravgrunnlag,
    val overstyrBehandletNettoTilBruttoMotTilbakekreving: JaNei?,
)

data class TilbakekrevingVurdering(
    val aarsak: TilbakekrevingAarsak?,
    val beskrivelse: String?,
    val forhaandsvarsel: TilbakekrevingVarsel?,
    val forhaandsvarselDato: LocalDate?,
    val doedsbosak: JaNei?,
    val foraarsaketAv: String?,
    val tilsvar: TilbakekrevingTilsvar?,
    val rettsligGrunnlag: TilbakekrevingHjemmel?,
    val objektivtVilkaarOppfylt: String?,
    val uaktsomtForaarsaketFeilutbetaling: String?,
    val burdeBrukerForstaatt: String?,
    val burdeBrukerForstaattEllerUaktsomtForaarsaket: String?,
    val vilkaarsresultat: TilbakekrevingVilkaar?,
    val beloepBehold: TilbakekrevingBeloepBehold?,
    val reduseringAvKravet: String?,
    val foreldet: String?,
    val rentevurdering: String?,
    val vedtak: String?,
    val vurderesForPaatale: String?,
)

enum class TilbakekrevingVarsel {
    EGET_BREV,
    MED_I_ENDRINGSBREV,
    AAPENBART_UNOEDVENDIG,
}

enum class JaNei {
    JA,
    NEI,
}

enum class TilbakekrevingVilkaar {
    OPPFYLT,
    DELVIS_OPPFYLT,
    IKKE_OPPFYLT,
}

data class TilbakekrevingTilsvar(
    val tilsvar: JaNei?,
    val dato: LocalDate?,
    val beskrivelse: String?,
)

data class TilbakekrevingBeloepBehold(
    val behold: TilbakekrevingBeloepBeholdSvar?,
    val beskrivelse: String?,
)

enum class TilbakekrevingBeloepBeholdSvar {
    BELOEP_I_BEHOLD,
    BELOEP_IKKE_I_BEHOLD,
}

data class TilbakekrevingPeriode(
    val maaned: YearMonth,
    val tilbakekrevingsbeloep: List<Tilbakekrevingsbelop>,
)

fun List<Tilbakekrevingsbelop>.kunYtelse() = filter { it.klasseType == KlasseType.YTEL.name }

val tilbakekrevingsbeloepComparator =
    compareBy<Tilbakekrevingsbelop> {
        when (it.klasseType) {
            KlasseType.FEIL.name -> 0
            KlasseType.YTEL.name -> 1
            else -> 2
        }
    }

data class Tilbakekrevingsbelop(
    val id: UUID,
    val klasseKode: String,
    val klasseType: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val skatteprosent: BigDecimal,
    val beregnetFeilutbetaling: Int?,
    val bruttoTilbakekreving: Int?,
    val nettoTilbakekreving: Int?,
    val skatt: Int?,
    val skyld: TilbakekrevingSkyld?,
    val resultat: TilbakekrevingResultat?,
    val tilbakekrevingsprosent: Int?,
    val rentetillegg: Int?,
)

fun List<KravgrunnlagPeriode>.tilTilbakekrevingPerioder(): List<TilbakekrevingPeriode> =
    map { periode ->
        TilbakekrevingPeriode(
            maaned = periode.periode.fraOgMed,
            tilbakekrevingsbeloep =
                periode.grunnlagsbeloep
                    .map {
                        Tilbakekrevingsbelop(
                            id = UUID.randomUUID(),
                            klasseKode = it.klasseKode.value,
                            klasseType = it.klasseType.name,
                            bruttoUtbetaling = it.bruttoUtbetaling.toInt(),
                            nyBruttoUtbetaling = it.nyBruttoUtbetaling.toInt(),
                            skatteprosent = it.skatteProsent,
                            bruttoTilbakekreving = it.bruttoTilbakekreving.toInt(),
                            beregnetFeilutbetaling = null,
                            nettoTilbakekreving = null,
                            skatt = null,
                            skyld = null,
                            resultat = null,
                            tilbakekrevingsprosent = null,
                            rentetillegg = null,
                        )
                    }.sortedWith(tilbakekrevingsbeloepComparator),
        )
    }

enum class TilbakekrevingAarsak {
    OMGJOERING,
    OPPHOER,
    REVURDERING,
    UTBFEILMOT,
    ANNET,
}

enum class TilbakekrevingHjemmel(
    val kode: String,
) {
    TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM("22-15-1-1"),
    TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM("22-15-1-2"),

    // Økonomi bruker ikke hjemmel-informasjonen til noe, og det finnes ingen egen kode for disse variantene.
    // Bruker den generelle hjemmelen.
    TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM("22-15"),
    TJUETO_FEMTEN_FEMTE_LEDD("22-15"),
}

enum class TilbakekrevingSkyld {
    BRUKER,
    IKKE_FORDELT,
    NAV,
    SKYLDDELING,
}

enum class TilbakekrevingResultat(
    private val gradAvTilbakekreving: Int,
) {
    FEILREGISTRERT(0),
    FORELDET(1),
    INGEN_TILBAKEKREV(2),
    DELVIS_TILBAKEKREV(3),
    FULL_TILBAKEKREV(4),
    ;

    companion object {
        fun hoyesteGradAvTilbakekreving(perioder: List<TilbakekrevingResultat>): TilbakekrevingResultat? =
            perioder.maxByOrNull { it.gradAvTilbakekreving }
    }
}

/*
* N.B Inneholder ikke alle vedtaksinfo kun det som er nødvendig for Tilbakekrevingskomponent.
*/
data class TilbakekrevingVedtak(
    val sakId: SakId,
    val vedtakId: Long,
    val fattetVedtak: FattetVedtak,
    val aarsak: TilbakekrevingAarsak,
    val hjemmel: TilbakekrevingHjemmel,
    val kravgrunnlagId: String,
    val kontrollfelt: String,
    val perioder: List<TilbakekrevingPeriode>,
    val overstyrBehandletNettoTilBruttoMotTilbakekreving: Boolean,
)

data class FattetVedtak(
    val saksbehandler: String,
    val enhet: Enhetsnummer,
    val dato: LocalDate,
)
