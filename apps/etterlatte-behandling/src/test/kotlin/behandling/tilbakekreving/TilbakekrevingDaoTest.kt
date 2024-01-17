package no.nav.etterlatte.behandling.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.DatabaseExtension
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
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingAktsomhet
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHjemmel
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurdering
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVurderingUaktsomhet
import no.nav.etterlatte.libs.common.tilbakekreving.UUID30
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
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
class TilbakekrevingDaoTest {
    private val dataSource: DataSource = DatabaseExtension.dataSource
    private lateinit var sakDao: SakDao
    private lateinit var tilbakekrevingDao: TilbakekrevingDao

    private lateinit var sak: Sak

    @BeforeAll
    fun setup() {
        val connection = dataSource.connection
        sakDao = SakDao { connection }
        tilbakekrevingDao = TilbakekrevingDao { connection }
    }

    @BeforeEach
    fun resetTabell() {
        dataSource.connection.prepareStatement("""TRUNCATE TABLE tilbakekrevingsperiode""")
            .executeUpdate()
        dataSource.connection.prepareStatement("""TRUNCATE TABLE tilbakekreving CASCADE""")
            .executeUpdate()
        dataSource.connection.prepareStatement("""TRUNCATE TABLE sak CASCADE """)
            .executeUpdate()
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
                tilbakekreving =
                    lagret.tilbakekreving.copy(
                        vurdering =
                            TilbakekrevingVurdering(
                                beskrivelse = "beskrivelse",
                                konklusjon = "konklusjon",
                                aarsak = TilbakekrevingAarsak.ANNET,
                                aktsomhet =
                                    TilbakekrevingVurderingUaktsomhet(
                                        aktsomhet = TilbakekrevingAktsomhet.GOD_TRO,
                                        reduseringAvKravet = "Redusering av kravet",
                                        strafferettsligVurdering = "Strafferettslig",
                                        rentevurdering = "Rentevurdering",
                                    ),
                                hjemmel = TilbakekrevingHjemmel.TJUETO_FEMTEN_EN_LEDD_EN,
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
