package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.tilbakekreving.vedtak.FattetVedtak
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingAarsak
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingPeriode
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingResultat
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingSkyld
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingVedtak
import no.nav.etterlatte.tilbakekreving.vedtak.TilbakekrevingVurdering
import no.nav.etterlatte.tilbakekreving.vedtak.Tilbakekrevingsbelop
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun readFile(file: String) =
    object {}.javaClass.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")

fun tilbakekrevingsvedtak(vedtakId: Long = 1) =
    TilbakekrevingVedtak(
        vedtakId = vedtakId,
        fattetVedtak =
            FattetVedtak(
                saksbehandler = "Z123456",
                enhet = "1234",
                dato = LocalDate.now(),
            ),
        perioder =
            listOf(
                TilbakekrevingPeriode(
                    maaned = YearMonth.of(2023, 1),
                    ytelse =
                        Tilbakekrevingsbelop(
                            klasseKode = "YTEL",
                            bruttoUtbetaling = 1000,
                            nyBruttoUtbetaling = 500,
                            skatteprosent = BigDecimal.valueOf(10),
                            beregnetFeilutbetaling = 500,
                            bruttoTilbakekreving = 500,
                            nettoTilbakekreving = 550,
                            skatt = 50,
                            skyld = TilbakekrevingSkyld.BRUKER,
                            resultat = TilbakekrevingResultat.FULL_TILBAKEKREV,
                            tilbakekrevingsprosent = 100,
                            rentetillegg = 50,
                        ),
                    feilkonto =
                        Tilbakekrevingsbelop(
                            klasseKode = "FEIL",
                            bruttoUtbetaling = 0,
                            nyBruttoUtbetaling = 0,
                            skatteprosent = BigDecimal.ZERO,
                            beregnetFeilutbetaling = 500,
                            bruttoTilbakekreving = 0,
                            nettoTilbakekreving = 0,
                            skatt = 0,
                            skyld = TilbakekrevingSkyld.BRUKER,
                            resultat = TilbakekrevingResultat.FULL_TILBAKEKREV,
                            tilbakekrevingsprosent = 0,
                            rentetillegg = 0,
                        ),
                ),
            ),
        vurdering =
            TilbakekrevingVurdering(
                aarsak = TilbakekrevingAarsak.DODSFALL,
                hjemmel = "hjemmel",
            ),
        kontrollfelt = "2023-09-19-10.01.03.842916",
    )
