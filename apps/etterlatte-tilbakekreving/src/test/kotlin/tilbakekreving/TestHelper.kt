package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.UUID30
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.FattetVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.Grunnlagsbeloep
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseKode
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.KravOgVedtakstatus
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.Periode
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingPeriodeVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingSkyld
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingsbelopFeilkontoVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingsbelopYtelseVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.libs.common.toUUID30
import java.io.FileNotFoundException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun readFile(file: String) =
    object {}.javaClass.getResource(file)?.readText()
        ?: throw FileNotFoundException("Fant ikke filen $file")

fun tilbakekrevingsvedtak(vedtakId: Long = 1) =
    TilbakekrevingVedtak(
        sakId = 1,
        vedtakId = vedtakId,
        fattetVedtak =
            FattetVedtak(
                saksbehandler = "Z123456",
                enhet = Enhet.defaultEnhet,
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

fun kravgrunnlag(
    sak: Sak = Sak("12345678901", SakType.BARNEPENSJON, 1L, Enhet.defaultEnhet),
    behandlingId: UUID30 = UUID.randomUUID().toUUID30(),
    status: KravgrunnlagStatus = KravgrunnlagStatus.NY,
    perioder: List<KravgrunnlagPeriode>? = null,
) = Kravgrunnlag(
    kravgrunnlagId = KravgrunnlagId(123L),
    sakId = SakId(sak.id),
    vedtakId = VedtakId(2L),
    kontrollFelt = Kontrollfelt(""),
    status = status,
    saksbehandler = NavIdent(""),
    referanse = behandlingId,
    perioder =
        perioder ?: listOf(
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

fun kravOgVedtakStatus(
    sak: Sak = Sak("12345678901", SakType.BARNEPENSJON, 1L, Enhet.defaultEnhet),
    behandlingId: UUID30 = UUID.randomUUID().toUUID30(),
    status: KravgrunnlagStatus = KravgrunnlagStatus.NY,
) = KravOgVedtakstatus(
    sakId = SakId(sak.id),
    vedtakId = VedtakId(2L),
    status = status,
    referanse = behandlingId,
)
