package no.nav.etterlatte.brev.model.tilbakekreving

import no.nav.etterlatte.libs.common.UUID30
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseKode
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.Periode
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVarsel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekrevingsbelop
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun tilbakekreving(
    vurdering: TilbakekrevingVurdering = tilbakekrevingvurdering(),
    perioder: List<TilbakekrevingPeriode> = emptyList(),
) = Tilbakekreving(
    vurdering = vurdering,
    perioder = perioder,
    kravgrunnlag = kravgrunnlag(),
    overstyrBehandletNettoTilBruttoMotTilbakekreving = null,
)

fun tilbakekrevingvurdering() =
    TilbakekrevingVurdering(
        aarsak = TilbakekrevingAarsak.ANNET,
        beskrivelse = "beskrivelse",
        forhaandsvarsel = TilbakekrevingVarsel.EGET_BREV,
        forhaandsvarselDato = LocalDate.of(2024, 1, 1),
        doedsbosak = null,
        foraarsaketAv = null,
        tilsvar = null,
        rettsligGrunnlag = TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM,
        objektivtVilkaarOppfylt = null,
        uaktsomtForaarsaketFeilutbetaling = null,
        burdeBrukerForstaatt = null,
        burdeBrukerForstaattEllerUaktsomtForaarsaket = null,
        vilkaarsresultat = null,
        beloepBehold = null,
        reduseringAvKravet = null,
        foreldet = null,
        rentevurdering = null,
        vedtak = "konklusjon",
        vurderesForPaatale = null,
    )

fun kravgrunnlag() =
    Kravgrunnlag(
        kravgrunnlagId = KravgrunnlagId(123L),
        sakId = SakId(474L),
        vedtakId = VedtakId(2L),
        kontrollFelt = Kontrollfelt(""),
        status = KravgrunnlagStatus.ANNU,
        saksbehandler = NavIdent(""),
        referanse = UUID30(""),
        perioder =
            listOf(
                KravgrunnlagPeriode(
                    periode =
                        Periode(
                            fraOgMed = YearMonth.of(2023, 1),
                            tilOgMed = YearMonth.of(2023, 2),
                        ),
                    skatt = BigDecimal(200),
                    grunnlagsbeloep =
                        listOf(
                            Grunnlagsbeloep(
                                klasseKode = KlasseKode(""),
                                klasseType = KlasseType.YTEL,
                                bruttoUtbetaling = BigDecimal(1000),
                                nyBruttoUtbetaling = BigDecimal(1200),
                                bruttoTilbakekreving = BigDecimal(200),
                                beloepSkalIkkeTilbakekreves = BigDecimal(200),
                                skatteProsent = BigDecimal(20),
                                resultat = null,
                                skyld = null,
                                aarsak = null,
                            ),
                            Grunnlagsbeloep(
                                klasseKode = KlasseKode(""),
                                klasseType = KlasseType.FEIL,
                                bruttoUtbetaling = BigDecimal(0),
                                nyBruttoUtbetaling = BigDecimal(0),
                                bruttoTilbakekreving = BigDecimal(0),
                                beloepSkalIkkeTilbakekreves = BigDecimal(0),
                                skatteProsent = BigDecimal(0),
                                resultat = null,
                                skyld = null,
                                aarsak = null,
                            ),
                        ),
                ),
            ),
    )

fun tilbakekrevingperiode(
    beregnetFeilutbetaling: Int = 0,
    bruttoTilbakekreving: Int = 0,
    nettoTilbakekreving: Int = 0,
    skatt: Int = 0,
    renteTilleg: Int = 0,
    resultat: TilbakekrevingResultat = TilbakekrevingResultat.FULL_TILBAKEKREV,
) = TilbakekrevingPeriode(
    maaned = YearMonth.of(2023, 1),
    tilbakekrevingsbeloep =
        listOf(
            Tilbakekrevingsbelop(
                id = UUID.randomUUID(),
                klasseKode = "",
                klasseType = "YTEL",
                bruttoUtbetaling = 0,
                nyBruttoUtbetaling = 0,
                skatteprosent = BigDecimal(10),
                beregnetFeilutbetaling = beregnetFeilutbetaling,
                bruttoTilbakekreving = bruttoTilbakekreving,
                nettoTilbakekreving = nettoTilbakekreving,
                skatt = skatt,
                skyld = null,
                resultat = resultat,
                tilbakekrevingsprosent = 0,
                rentetillegg = renteTilleg,
            ),
            Tilbakekrevingsbelop(
                id = UUID.randomUUID(),
                klasseKode = "",
                klasseType = "FEIL",
                bruttoUtbetaling = 0,
                nyBruttoUtbetaling = 0,
                skatteprosent = BigDecimal(10),
                beregnetFeilutbetaling = 0,
                bruttoTilbakekreving = 0,
                nettoTilbakekreving = 0,
                skatt = 0,
                skyld = null,
                resultat = TilbakekrevingResultat.FULL_TILBAKEKREV,
                tilbakekrevingsprosent = 0,
                rentetillegg = 0,
            ),
        ),
)
