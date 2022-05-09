package no.nav.etterlatte.itest

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")


    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Sletting av alle behandlinger i en sak`() {
        val connection = dataSource.connection
        val sakrepo = SakDao { connection }
        val behandlingRepo = BehandlingDao { connection }

        val sak1 = sakrepo.opprettSak("123", "BP").id
        val sak2 = sakrepo.opprettSak("321", "BP").id
        listOf(
            Behandling(UUID.randomUUID(), sak1, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, emptyList(), emptyList(),
                emptyList(), null, BehandlingStatus.OPPRETTET ),
            Behandling(UUID.randomUUID(), sak1, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, emptyList(), emptyList(),
                emptyList(), null, BehandlingStatus.OPPRETTET ),
            Behandling(UUID.randomUUID(), sak2, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, emptyList(), emptyList(),
                emptyList(), null, BehandlingStatus.OPPRETTET ),
        ).forEach { b ->
            behandlingRepo.opprett(b)
        }

        Assertions.assertEquals(2, behandlingRepo.alleBehandingerISak(sak1).size)

        behandlingRepo.slettBehandlingerISak(sak1)

        Assertions.assertEquals(0, behandlingRepo.alleBehandingerISak(sak1).size)
        Assertions.assertEquals(1, behandlingRepo.alleBehandingerISak(sak2).size)

        connection.close()
    }

/*
    @Test
    fun `avbryte sak`() {
        val connection = dataSource.connection
        val sakrepo = SakDao { connection }
        val behandlingRepo = BehandlingDao { connection }

        val sak1 = sakrepo.opprettSak("123", "BP").id
        listOf(
            Behandling(UUID.randomUUID(), sak1, listOf(ikkeDeltOpplysning), null, null, null, false),
        ).forEach { b ->
            behandlingRepo.opprett(b)
            b.grunnlag.forEach { o -> opplysningRepo.leggOpplysningTilBehandling(b.id, o.id) }
        }

        Assertions.assertEquals(1, behandlingRepo.alleISak(sak1).size)

        var behandling = behandlingRepo.hentBehandlingerMedSakId(sak1)
        Assertions.assertEquals(1, behandling.size)
        Assertions.assertEquals(false, behandling.first().avbrutt)

        behandlingRepo.avbrytBehandling(behandling.first())
        behandling = behandlingRepo.hentBehandlingerMedSakId(sak1)
        Assertions.assertEquals(1, behandling.size)
        Assertions.assertEquals(true, behandling.first().avbrutt)

        connection.close()
    }

 */

}

