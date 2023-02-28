package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoReguleringTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingRepo: BehandlingDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).apply { migrate() }

        val connection = dataSource.connection
        sakRepo = SakDao { connection }
        behandlingRepo = BehandlingDao { connection }
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE behandling CASCADE;").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private fun hentMigrerbareStatuses() =
        BehandlingStatus.values().toList() - BehandlingStatus.skalIkkeOmberegnesVedGRegulering().toSet()

    @ParameterizedTest(
        name = "behandling med status {0} skal endres til aa vaere VILKAARSVURDERT"
    )
    @MethodSource("hentMigrerbareStatuses")
    fun `behandlinger som er beregnet maa beregnes paa nytt`(status: BehandlingStatus) {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling =
            opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak, status = status)

        behandlingRepo.opprettBehandling(opprettBehandling)
        behandlingRepo.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning()

        with(behandlingRepo.hentBehandling(opprettBehandling.id)) {
            val expected = BehandlingStatus.VILKAARSVURDERT
            val actual = (this as Foerstegangsbehandling).status

            assertEquals(expected, actual)
        }
    }

    private fun hentStatuser() = BehandlingStatus.skalIkkeOmberegnesVedGRegulering()

    @ParameterizedTest(name = "behandling med status {0} skal fortsette aa ha samme status ved migrering")
    @MethodSource("hentStatuser")
    fun `irrelevante behandlinger skal ikke endre status ved migrering`(status: BehandlingStatus) {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling =
            opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak, status = status)

        behandlingRepo.opprettBehandling(opprettBehandling)
        behandlingRepo.migrerStatusPaaAlleBehandlingerSomTrengerNyBeregning()

        with(behandlingRepo.hentBehandling(opprettBehandling.id)) {
            val expected = status
            val actual = (this as Foerstegangsbehandling).status

            assertEquals(expected, actual)
        }
    }
}