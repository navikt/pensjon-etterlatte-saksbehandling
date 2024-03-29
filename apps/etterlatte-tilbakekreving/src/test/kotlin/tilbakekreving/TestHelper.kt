package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.libs.common.tilbakekreving.FattetVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriodeVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingSkyld
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingsbelopFeilkontoVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingsbelopYtelseVedtak
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
                TilbakekrevingPeriodeVedtak(
                    maaned = YearMonth.of(2023, 1),
                    ytelse =
                        TilbakekrevingsbelopYtelseVedtak(
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
                        TilbakekrevingsbelopFeilkontoVedtak(
                            klasseKode = "FEIL",
                            bruttoUtbetaling = 0,
                            nyBruttoUtbetaling = 500,
                            bruttoTilbakekreving = 0,
                        ),
                ),
            ),
        aarsak = TilbakekrevingAarsak.ANNET,
        hjemmel = TilbakekrevingHjemmel.TJUETO_FEMTEN_FEMTE_LEDD,
        kravgrunnlagId = "1",
        kontrollfelt = "2023-09-19-10.01.03.842916",
    )
