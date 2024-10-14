package behandling.tilbakekreving

import no.nav.etterlatte.libs.common.UUID30
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.JaNei
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
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBeloepBehold
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBeloepBeholdSvar
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingTilsvar
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVarsel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVilkaar
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.libs.common.toUUID30
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun kravgrunnlag(
    sak: Sak,
    behandlingId: UUID30 = UUID.randomUUID().toUUID30(),
) = Kravgrunnlag(
    kravgrunnlagId = KravgrunnlagId(123L),
    sakId = SakId(sak.id.sakId),
    vedtakId = VedtakId(2L),
    kontrollFelt = Kontrollfelt(""),
    status = KravgrunnlagStatus.NY,
    saksbehandler = NavIdent(""),
    referanse = behandlingId,
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

fun tilbakekrevingVurdering(
    beskrivelse: String? = "en beskrivelse",
    rettsligGrunnlag: TilbakekrevingHjemmel? = TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_PUNKTUM,
) = TilbakekrevingVurdering(
    aarsak = TilbakekrevingAarsak.REVURDERING,
    beskrivelse = beskrivelse,
    forhaandsvarsel = TilbakekrevingVarsel.EGET_BREV,
    forhaandsvarselDato = LocalDate.of(2024, 1, 1),
    doedsbosak = JaNei.NEI,
    foraarsaketAv = "Denne ble forårsaket av bruker.",
    tilsvar =
        TilbakekrevingTilsvar(
            tilsvar = JaNei.JA,
            beskrivelse = "Tilsvar på varsel.",
            dato = LocalDate.of(2024, 1, 15),
        ),
    rettsligGrunnlag = rettsligGrunnlag,
    objektivtVilkaarOppfylt = "Ja.",
    uaktsomtForaarsaketFeilutbetaling = null,
    burdeBrukerForstaatt = "Ja",
    burdeBrukerForstaattEllerUaktsomtForaarsaket = null,
    vilkaarsresultat = TilbakekrevingVilkaar.IKKE_OPPFYLT,
    beloepBehold =
        TilbakekrevingBeloepBehold(
            behold = TilbakekrevingBeloepBeholdSvar.BELOEP_IKKE_I_BEHOLD,
            beskrivelse = "Beløpet er ikke i behold.",
        ),
    reduseringAvKravet = null,
    foreldet = null,
    rentevurdering = null,
    vedtak = "Bruker må betale tilbake.",
    vurderesForPaatale = null,
)
