package no.nav.etterlatte.behandling.tilbakekreving

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
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
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAarsak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.tilbakekreving.UUID30
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.mockedSakTilgangDao
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class TilbakekrevingDaoTest(val dataSource: DataSource) {
    private lateinit var sakDao: SakDao
    private lateinit var tilbakekrevingDao: TilbakekrevingDao

    private lateinit var sak: Sak

    @BeforeAll
    fun setup() {
        sakDao = SakDao(ConnectionAutoclosingTest(dataSource))
        tilbakekrevingDao = TilbakekrevingDao(ConnectionAutoclosingTest(dataSource))
        val user = mockk<SaksbehandlerMedEnheterOgRoller>()
        Kontekst.set(
            Context(
                user,
                DatabaseContextTest(dataSource),
                mockedSakTilgangDao(),
            ),
        )
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.use {
            it.prepareStatement("""TRUNCATE TABLE tilbakekrevingsperiode""").executeUpdate()
            it.prepareStatement("""TRUNCATE TABLE tilbakekreving CASCADE""").executeUpdate()
            it.prepareStatement("""TRUNCATE TABLE sak CASCADE """).executeUpdate()
        }
        sak = sakDao.opprettSak(fnr = "en bruker", type = SakType.OMSTILLINGSSTOENAD, enhet = "1337")
    }

    @Test
    fun `Lagre ny tilbakekreving`() {
        val ny = tilbakekreving(sak)
        val lagret = tilbakekrevingDao.lagreTilbakekreving(ny)
        lagret shouldBe ny
    }

    @Test
    fun `Lagre eksisterende tilbakekreving`() {
        val ny = tilbakekreving(sak)
        val lagret = tilbakekrevingDao.lagreTilbakekreving(ny)
        val oppdatert =
            lagret.copy(
                status = TilbakekrevingStatus.UNDER_ARBEID,
                sendeBrev = true,
                tilbakekreving =
                    lagret.tilbakekreving.copy(
                        vurdering =
                            TilbakekrevingVurdering(
                                aarsak = TilbakekrevingAarsak.ANNET,
                                beskrivelse = "beskrivelse",
                                forhaandsvarsel = null,
                                forhaandsvarselDato = null,
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
                                hjemmel = TilbakekrevingHjemmel.TJUETO_FEMTEN_FOERSTE_LEDD_FOERSTE_OG_ANDRE_PUNKTUM,
                            ),
                        perioder =
                            lagret.tilbakekreving.perioder.map {
                                it.copy(
                                    ytelse = it.ytelse.copy(beregnetFeilutbetaling = 123),
                                )
                            },
                    ),
            )
        val lagretOppdatert = tilbakekrevingDao.lagreTilbakekreving(oppdatert)
        lagretOppdatert shouldBe oppdatert
    }

    @Test
    fun `Hente tilbakekrevinger med sakid`() {
        tilbakekrevingDao.lagreTilbakekreving(tilbakekreving(sak))
        tilbakekrevingDao.lagreTilbakekreving(tilbakekreving(sak))
        val tilbakekrevinger = tilbakekrevingDao.hentTilbakekrevinger(sak.id)
        tilbakekrevinger.size shouldBe 2
        tilbakekrevinger.forEach {
            it.tilbakekreving.perioder.size shouldBe 1
        }
    }

    companion object {
        private fun tilbakekreving(sak: Sak) =
            TilbakekrevingBehandling.ny(
                sak = sak,
                kravgrunnlag =
                    Kravgrunnlag(
                        kravgrunnlagId = KravgrunnlagId(123L),
                        sakId = SakId(474L),
                        vedtakId = VedtakId(2L),
                        kontrollFelt = Kontrollfelt(""),
                        status = KravgrunnlagStatus.ANNU,
                        saksbehandler = NavIdent(""),
                        sisteUtbetalingslinjeId = UUID30(""),
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
                    ),
            )
    }
}
