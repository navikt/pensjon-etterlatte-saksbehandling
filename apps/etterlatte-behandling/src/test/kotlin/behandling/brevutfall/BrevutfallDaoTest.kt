package no.nav.etterlatte.behandling.brevutfall

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.OpprettBehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BrevutfallDaoTest {
    private lateinit var dataSource: DataSource
    private lateinit var behandlingDao: BehandlingDao
    private lateinit var sakDao: SakDao
    private lateinit var dao: BrevutfallDao

    private lateinit var behandlingId: UUID

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    @BeforeAll
    fun setup() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
                maxPoolSize = 20,
            ).apply { migrate() }

        sakDao = SakDao { dataSource.connection }
        behandlingDao =
            BehandlingDao(
                KommerBarnetTilGodeDao { dataSource.connection },
                RevurderingDao { dataSource.connection },
            ) { dataSource.connection }

        dao = BrevutfallDao { dataSource.connection }
    }

    @BeforeEach
    fun reset() {
        dataSource.connection.prepareStatement("""TRUNCATE TABLE brevutfall""").executeUpdate()

        val sak = opprettSakForTest()
        opprettBehandlingForTest(sak).let {
            behandlingDao.opprettBehandling(it)
            behandlingId = it.id
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal lagre brevutfall`() {
        val brevutfall = brevutfall(behandlingId)

        val lagretBrevutfall = dao.lagre(brevutfall)

        lagretBrevutfall shouldNotBe null
        lagretBrevutfall.etterbetaling?.fom shouldBe brevutfall.etterbetaling?.fom
        lagretBrevutfall.etterbetaling?.tom shouldBe brevutfall.etterbetaling?.tom
        lagretBrevutfall.aldersgruppe shouldBe brevutfall.aldersgruppe
        lagretBrevutfall.kilde shouldBe brevutfall.kilde
    }

    @Test
    fun `skal hente brevutfall`() {
        val brevutfall = brevutfall(behandlingId)

        dao.lagre(brevutfall)
        val lagretBrevutfall = dao.hent(brevutfall.behandlingId)

        lagretBrevutfall shouldNotBe null
    }

    @Test
    fun `skal oppdatere brevutfall`() {
        val brevutfall = brevutfall(behandlingId)

        val lagretBrevutfall = dao.lagre(brevutfall)

        val oppdatertBrevutfall =
            lagretBrevutfall.copy(
                etterbetaling =
                    Etterbetaling(
                        fom = brevutfall.etterbetaling!!.fom.minusYears(1),
                        tom = brevutfall.etterbetaling!!.tom.minusYears(1),
                    ),
                aldersgruppe = Aldersgruppe.OVER_18,
            )

        val lagretOppdatertBrevutfall = dao.lagre(oppdatertBrevutfall)

        lagretOppdatertBrevutfall shouldNotBe null
        lagretOppdatertBrevutfall.etterbetaling?.fom shouldBe brevutfall.etterbetaling?.fom?.minusYears(1)
        lagretOppdatertBrevutfall.etterbetaling?.tom shouldBe brevutfall.etterbetaling?.tom?.minusYears(1)
        lagretOppdatertBrevutfall.aldersgruppe shouldBe Aldersgruppe.OVER_18
        lagretOppdatertBrevutfall.kilde shouldNotBe null
    }

    private fun brevutfall(behandlingId: UUID) =
        Brevutfall(
            behandlingId = behandlingId,
            etterbetaling =
                Etterbetaling(
                    fom = YearMonth.of(2023, 11),
                    tom = YearMonth.of(2023, 12),
                ),
            aldersgruppe = Aldersgruppe.UNDER_18,
            kilde = Grunnlagsopplysning.Saksbehandler("Z1234567", Tidspunkt.now()),
        )

    private fun opprettSakForTest() =
        sakDao.opprettSak(
            fnr = "12345678910",
            type = SakType.BARNEPENSJON,
            enhet = "1234",
        )

    private fun opprettBehandlingForTest(sak: Sak) =
        OpprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak.id,
            status = BehandlingStatus.OPPRETTET,
            kilde = Vedtaksloesning.GJENNY,
        )
}
