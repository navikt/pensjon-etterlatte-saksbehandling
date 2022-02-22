package no.nav.etterlatte.itest

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
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
        val sakrepo = SakDao{connection}
        val behandlingRepo = BehandlingDao { connection }
        val opplysningRepo = OpplysningDao { connection }
        val deltOpplysning = Behandlingsopplysning(UUID.randomUUID(), Behandlingsopplysning.Pdl("pdl", Instant.now(), null), "test", objectMapper.createObjectNode(), objectMapper.createObjectNode()).also { opplysningRepo.nyOpplysning(it) }
        val ikkeDeltOpplysning = Behandlingsopplysning(UUID.randomUUID(), Behandlingsopplysning.Pdl("pdl", Instant.now(), null), "test", objectMapper.createObjectNode(), objectMapper.createObjectNode()).also { opplysningRepo.nyOpplysning(it) }
        val sak1 = sakrepo.opprettSak("123", "BP").id
        val sak2 = sakrepo.opprettSak("321", "BP").id
        listOf(
            Behandling(UUID.randomUUID(), sak1, listOf(deltOpplysning), null, null, false),
            Behandling(UUID.randomUUID(), sak1, listOf(ikkeDeltOpplysning), null, null, false),
            Behandling(UUID.randomUUID(), sak2, listOf(deltOpplysning), null, null, false)
        ).forEach {b ->
            behandlingRepo.opprett(b)
            b.grunnlag.forEach { o-> opplysningRepo.leggOpplysningTilBehandling(b.id, o.id)}
        }

        Assertions.assertEquals(2, behandlingRepo.alleISak(sak1).size)


        opplysningRepo.slettOpplysningerISak(sak1)
        behandlingRepo.slettBehandlingerISak(sak1)

        Assertions.assertEquals(0, behandlingRepo.alleISak(sak1).size)
        Assertions.assertEquals(1, behandlingRepo.alleISak(sak2).size)
        Assertions.assertEquals(1, opplysningRepo.finnOpplysningerIBehandling(behandlingRepo.alleISak(sak2).first().id).size)

        connection.close()
    }

}
