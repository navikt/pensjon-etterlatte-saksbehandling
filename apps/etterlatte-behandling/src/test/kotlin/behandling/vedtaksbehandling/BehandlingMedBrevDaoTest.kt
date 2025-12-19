package no.nav.etterlatte.behandling.vedtaksbehandling

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.klage.KlageDao
import no.nav.etterlatte.behandling.klage.KlageDaoImpl
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.behandling.tilbakekreving.TilbakekrevingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.UUID30
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.Kontrollfelt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagId
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagStatus
import no.nav.etterlatte.libs.common.tilbakekreving.NavIdent
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class BehandlingMedBrevDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var sakRepo: SakSkrivDao
    private lateinit var behandlingMedBrevDao: BehandlingMedBrevDao
    private lateinit var behandlingDao: BehandlingDao
    private lateinit var klageDao: KlageDao
    private lateinit var tilbakekrevingDao: TilbakekrevingDao

    @BeforeAll
    fun beforeAll() {
        val kommerBarnetTilGodeDao = KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource))
        val revurderingDao = RevurderingDao(ConnectionAutoclosingTest(dataSource))
        sakRepo = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)))
        behandlingMedBrevDao = BehandlingMedBrevDao(ConnectionAutoclosingTest(dataSource))
        behandlingDao = BehandlingDao(kommerBarnetTilGodeDao, revurderingDao, ConnectionAutoclosingTest(dataSource))
        klageDao = KlageDaoImpl(ConnectionAutoclosingTest(dataSource))
        tilbakekrevingDao = TilbakekrevingDao(ConnectionAutoclosingTest(dataSource))
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE behandling CASCADE;").execute()
        }
    }

    @Test
    fun `erBehandlingRedigerbar skal sjekke status paa behandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val behandling: Foerstegangsbehandling =
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak1,
            ).also { behandlingDao.opprettBehandling(it) }
                .let { requireNotNull(behandlingDao.hentBehandling(it.id)) as Foerstegangsbehandling }

        behandlingMedBrevDao.erBehandlingRedigerbar(behandling.id) shouldBe true

        behandlingDao.avbrytBehandling(behandling.id, null, null)

        behandlingMedBrevDao.erBehandlingRedigerbar(behandling.id) shouldBe false
    }

    @Test
    fun `erBehandlingRedigerbar skal sjekke status paa klage`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)

        val klage =
            Klage
                .ny(sak1, InnkommendeKlage(LocalDate.now(), "JP-24601", "Jean"))

        klageDao.lagreKlage(klage)

        behandlingMedBrevDao.erBehandlingRedigerbar(klage.id) shouldBe true

        klageDao.lagreKlage(klage.avbryt(AarsakTilAvbrytelse.ANNET))

        behandlingMedBrevDao.erBehandlingRedigerbar(klage.id) shouldBe false
    }

    @Test
    fun `erBehandlingRedigerbar skal sjekke status paa tilbakekreving`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr)

        val tilbakekreving =
            tilbakekreving(sak1)
                .copy(status = TilbakekrevingStatus.OPPRETTET)

        tilbakekrevingDao.lagreTilbakekreving(tilbakekreving)

        behandlingMedBrevDao.erBehandlingRedigerbar(tilbakekreving.id) shouldBe true

        tilbakekrevingDao.lagreTilbakekreving(tilbakekreving.copy(status = TilbakekrevingStatus.ATTESTERT))

        behandlingMedBrevDao.erBehandlingRedigerbar(tilbakekreving.id) shouldBe false
    }

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
                    referanse = UUID30(""),
                    perioder = emptyList(),
                ),
            omgjoeringAvId = null,
        )
}
