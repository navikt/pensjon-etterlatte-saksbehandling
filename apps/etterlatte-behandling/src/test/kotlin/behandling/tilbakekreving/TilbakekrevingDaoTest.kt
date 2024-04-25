package no.nav.etterlatte.behandling.tilbakekreving

import behandling.tilbakekreving.kravgrunnlag
import behandling.tilbakekreving.tilbakekrevingVurdering
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
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.mockedSakTilgangDao
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
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
                        vurdering = tilbakekrevingVurdering(),
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
                kravgrunnlag = kravgrunnlag(sak),
            )
    }
}
