package no.nav.etterlatte.behandling.behandlinginfo

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.EtterbetalingPeriodeValg
import no.nav.etterlatte.libs.common.behandling.Feilutbetaling
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class BehandlingInfoDaoTest(
    val dataSource: DataSource,
) {
    private lateinit var behandlingDao: BehandlingDao
    private lateinit var sakSkrivDao: SakSkrivDao
    private lateinit var dao: BehandlingInfoDao

    private lateinit var behandlingId: UUID

    @BeforeAll
    fun setup() {
        sakSkrivDao = SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() })
        behandlingDao =
            BehandlingDao(
                KommerBarnetTilGodeDao(ConnectionAutoclosingTest(dataSource)),
                RevurderingDao(ConnectionAutoclosingTest(dataSource)),
                ConnectionAutoclosingTest(dataSource),
            )

        dao = BehandlingInfoDao(ConnectionAutoclosingTest(dataSource))
    }

    @BeforeEach
    fun reset() {
        dataSource.connection.use { it.prepareStatement("""TRUNCATE TABLE behandling_info""").executeUpdate() }

        val sak = opprettSakForTest()
        opprettBehandlingForTest(sak).let {
            behandlingDao.opprettBehandling(it)
            behandlingId = it.id
        }
    }

    @Test
    fun `skal lagre erOmgjoeringSluttbehandlingUtland`() {
        val sluttbehandlingUtland = true
        dao.hentErOmgjoeringSluttbehandlingUtland(behandlingId) shouldBe !sluttbehandlingUtland
        dao.lagreErOmgjoeringSluttbehandlingUtland(behandlingId, sluttbehandlingUtland)
        dao.hentErOmgjoeringSluttbehandlingUtland(behandlingId) shouldBe sluttbehandlingUtland
    }

    @Test
    fun `skal lagre brevutfall`() {
        val brevutfall = brevutfall(behandlingId)

        val lagretBrevutfall = dao.lagreBrevutfall(brevutfall)

        lagretBrevutfall shouldNotBe null
        lagretBrevutfall.aldersgruppe shouldBe brevutfall.aldersgruppe
        lagretBrevutfall.kilde shouldBe brevutfall.kilde
    }

    @Test
    fun `skal hente brevutfall`() {
        val brevutfall = brevutfall(behandlingId)

        dao.lagreBrevutfall(brevutfall)
        val lagretBrevutfall = dao.hentBrevutfall(brevutfall.behandlingId)

        lagretBrevutfall shouldNotBe null
    }

    @Test
    fun `skal oppdatere brevutfall`() {
        val brevutfall = brevutfall(behandlingId)

        val lagretBrevutfall = dao.lagreBrevutfall(brevutfall)

        val oppdatertBrevutfall =
            lagretBrevutfall.copy(
                aldersgruppe = Aldersgruppe.OVER_18,
            )

        val lagretOppdatertBrevutfall = dao.lagreBrevutfall(oppdatertBrevutfall)

        lagretOppdatertBrevutfall shouldNotBe null
        lagretOppdatertBrevutfall.aldersgruppe shouldBe Aldersgruppe.OVER_18
        lagretOppdatertBrevutfall.kilde shouldNotBe null
    }

    @Test
    fun `skal lagre etterbetaling`() {
        val etterbetaling = etterbetaling(behandlingId)

        val lagretEtterbetaling = dao.lagreEtterbetaling(etterbetaling)

        lagretEtterbetaling shouldNotBe null
        lagretEtterbetaling.fom shouldBe etterbetaling.fom
        lagretEtterbetaling.tom shouldBe etterbetaling.tom
        lagretEtterbetaling.kilde shouldBe etterbetaling.kilde
    }

    @Test
    fun `skal hente etterbetaling`() {
        val etterbetaling = etterbetaling(behandlingId)

        dao.lagreEtterbetaling(etterbetaling)
        val etterbetalingHentet = dao.hentEtterbetaling(etterbetaling.behandlingId)

        etterbetalingHentet shouldNotBe null
    }

    @Test
    fun `skal slette etterbetaling`() {
        val etterbetaling = etterbetaling(behandlingId)

        val lagretEtterbetaling = dao.lagreEtterbetaling(etterbetaling)

        lagretEtterbetaling shouldNotBe null

        dao.slettEtterbetaling(etterbetaling.behandlingId)

        dao.hentEtterbetaling(etterbetaling.behandlingId) shouldBe null
    }

    private fun brevutfall(behandlingId: UUID) =
        Brevutfall(
            behandlingId = behandlingId,
            aldersgruppe = Aldersgruppe.UNDER_18,
            feilutbetaling = Feilutbetaling(FeilutbetalingValg.NEI, null),
            kilde = Grunnlagsopplysning.Saksbehandler("Z1234567", Tidspunkt.now()),
            frivilligSkattetrekk = true,
        )

    private fun etterbetaling(behandlingId: UUID) =
        Etterbetaling(
            behandlingId = behandlingId,
            fom = YearMonth.of(2023, 11),
            tom = YearMonth.of(2023, 12),
            inneholderKrav = true,
            frivilligSkattetrekk = true,
            etterbetalingPeriodeValg = EtterbetalingPeriodeValg.UNDER_3_MND,
            kilde = Grunnlagsopplysning.Saksbehandler("Z1234567", Tidspunkt.now()),
        )

    private fun opprettSakForTest() =
        sakSkrivDao.opprettSak(
            fnr = "12345678910",
            type = SakType.BARNEPENSJON,
            enhet = Enheter.defaultEnhet.enhetNr,
        )

    private fun opprettBehandlingForTest(sak: Sak) =
        OpprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak.id,
            status = BehandlingStatus.OPPRETTET,
            kilde = Vedtaksloesning.GJENNY,
            sendeBrev = true,
        )
}
