package no.nav.etterlatte.libs.common.tilbakekreving

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class Tilbakekreving(
    val vurdering: TilbakekrevingVurdering?,
    val perioder: List<TilbakekrevingPeriode>,
    val kravgrunnlag: Kravgrunnlag,
)

data class TilbakekrevingVurdering(
    val aarsak: TilbakekrevingAarsak?,
    val beskrivelse: String?,
    val forhaandsvarsel: TilbakekrevingVarsel?,
    val forhaandsvarselDato: LocalDate?,
    val doedsbosak: JaNei?,
    val foraarsaketAv: String?,
    val tilsvar: TilbakekrevingTilsvar?,
    val rettsligGrunnlag: TilbakekrevingRettsligGrunnlag?,
    val objektivtVilkaarOppfylt: String?,
    val subjektivtVilkaarOppfylt: String?,
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

data class TilbakekrevingVurderingUaktsomhet(
    val aktsomhet: TilbakekrevingAktsomhet?,
    val reduseringAvKravet: String?,
    val strafferettsligVurdering: String?,
    val rentevurdering: String?,
)

data class TilbakekrevingPeriode(
    val maaned: YearMonth,
    val ytelse: Tilbakekrevingsbelop,
    val feilkonto: Tilbakekrevingsbelop,
)

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
) {
    companion object {
        fun ytelse(grunnlagsbeloep: Grunnlagsbeloep) =
            Tilbakekrevingsbelop(
                id = UUID.randomUUID(),
                klasseKode = grunnlagsbeloep.klasseKode.value,
                klasseType = grunnlagsbeloep.klasseType.name,
                bruttoUtbetaling = grunnlagsbeloep.bruttoUtbetaling.toInt(),
                nyBruttoUtbetaling = grunnlagsbeloep.nyBruttoUtbetaling.toInt(),
                skatteprosent = grunnlagsbeloep.skatteProsent,
                beregnetFeilutbetaling = null,
                bruttoTilbakekreving = null,
                nettoTilbakekreving = null,
                skatt = null,
                skyld = grunnlagsbeloep.skyld?.let { TilbakekrevingSkyld.valueOf(it) },
                resultat = grunnlagsbeloep.resultat?.let { TilbakekrevingResultat.valueOf(it) },
                tilbakekrevingsprosent = null,
                rentetillegg = null,
            )

        fun feilkonto(grunnlagsbeloep: Grunnlagsbeloep) =
            Tilbakekrevingsbelop(
                id = UUID.randomUUID(),
                klasseKode = grunnlagsbeloep.klasseKode.value,
                klasseType = grunnlagsbeloep.klasseType.name,
                bruttoUtbetaling = grunnlagsbeloep.bruttoUtbetaling.toInt(),
                nyBruttoUtbetaling = grunnlagsbeloep.nyBruttoUtbetaling.toInt(),
                skatteprosent = grunnlagsbeloep.skatteProsent,
                beregnetFeilutbetaling = null,
                bruttoTilbakekreving = grunnlagsbeloep.bruttoTilbakekreving.toInt(),
                nettoTilbakekreving = null,
                skatt = null,
                skyld = null,
                resultat = null,
                tilbakekrevingsprosent = null,
                rentetillegg = null,
            )
    }

    fun toYtelseVedtak() =
        TilbakekrevingsbelopYtelseVedtak(
            klasseKode = klasseKode,
            bruttoUtbetaling = bruttoUtbetaling,
            nyBruttoUtbetaling = nyBruttoUtbetaling,
            skatteprosent = skatteprosent,
            beregnetFeilutbetaling = requireNotNull(beregnetFeilutbetaling),
            bruttoTilbakekreving = requireNotNull(bruttoTilbakekreving),
            nettoTilbakekreving = requireNotNull(nettoTilbakekreving),
            skatt = requireNotNull(skatt),
            skyld = requireNotNull(skyld),
            resultat = requireNotNull(resultat),
            tilbakekrevingsprosent = requireNotNull(tilbakekrevingsprosent),
            rentetillegg = requireNotNull(rentetillegg),
        )

    fun toFeilkontoVedtak() =
        TilbakekrevingsbelopFeilkontoVedtak(
            klasseKode = klasseKode,
            bruttoUtbetaling = bruttoUtbetaling,
            nyBruttoUtbetaling = nyBruttoUtbetaling,
            bruttoTilbakekreving = requireNotNull(bruttoTilbakekreving),
        )
}

fun List<KravgrunnlagPeriode>.tilTilbakekrevingPerioder(): List<TilbakekrevingPeriode> {
    return map { periode ->
        val ytelse =
            requireNotNull(periode.grunnlagsbeloep.find { it.klasseType == KlasseType.YTEL }) { "Fant ingen ytelse" }

        val feilkonto =
            requireNotNull(periode.grunnlagsbeloep.find { it.klasseType == KlasseType.FEIL }) { "Fant ikke feilkonto" }

        TilbakekrevingPeriode(
            maaned = periode.periode.fraOgMed,
            ytelse = Tilbakekrevingsbelop.ytelse(ytelse),
            feilkonto = Tilbakekrevingsbelop.feilkonto(feilkonto),
        )
    }
}

enum class TilbakekrevingAarsak {
    OMGJOERING,
    OPPHOER,
    REVURDERING,
    UTBFEILMOT,
    ANNET,
}

enum class TilbakekrevingAktsomhet {
    GOD_TRO,
    SIMPEL_UAKTSOMHET,
    GROV_UAKTSOMHET,
}

enum class TilbakekrevingRettsligGrunnlag(val kode: String) {
    TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM("22-15-1-1"),
    TJUETO_FEMTEN_FOERSTE_LEDD_ANDRE_PUNKTUM("22-15-1-2"),
    TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM("22-15-1-1/22-15-1-2"),
    TJUETO_FEMTEN_FEMTE_LEDD("22-15-5"),
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

/*
* N.B Inneholder ikke alle vedtaksinfo kun det som er nødvendig for Tilbakekrevingskomponent.
*/
data class TilbakekrevingVedtak(
    val vedtakId: Long,
    val fattetVedtak: FattetVedtak,
    val aarsak: TilbakekrevingAarsak,
    val hjemmel: TilbakekrevingRettsligGrunnlag,
    val kravgrunnlagId: String,
    val kontrollfelt: String,
    val perioder: List<TilbakekrevingPeriodeVedtak>,
)

data class FattetVedtak(
    val saksbehandler: String,
    val enhet: String,
    val dato: LocalDate,
)

data class TilbakekrevingPeriodeVedtak(
    val maaned: YearMonth,
    val ytelse: TilbakekrevingsbelopYtelseVedtak,
    val feilkonto: TilbakekrevingsbelopFeilkontoVedtak,
)

data class TilbakekrevingsbelopYtelseVedtak(
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

data class TilbakekrevingsbelopFeilkontoVedtak(
    val klasseKode: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val bruttoTilbakekreving: Int,
)
