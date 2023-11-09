package no.nav.etterlatte.behandling.bosattutland

import behandling.utland.LandMedDokumenter
import behandling.utland.MottattDokument
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BosattUtlandDaoTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var dataSource: DataSource
    private lateinit var bosattUtlandDao: BosattUtlandDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource =
            DataSourceBuilder.createDataSource(
                jdbcUrl = postgreSQLContainer.jdbcUrl,
                username = postgreSQLContainer.username,
                password = postgreSQLContainer.password,
            ).apply { migrate() }
        val connection = dataSource.connection
        bosattUtlandDao = BosattUtlandDao { connection }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `kan lagre og hente bosattutland`() {
        val seder =
            listOf(
                LandMedDokumenter(
                    landIsoKode = "AFG",
                    dokumenter =
                        listOf(
                            MottattDokument(
                                dokumenttype = "P2000",
                                dato = LocalDate.now(),
                                kommentar = "kom",
                            ),
                        ),
                ),
            )
        val behandlingid = UUID.randomUUID()
        val bosattUtland = BosattUtland(behandlingid, "rinannumer", seder, seder)
        bosattUtlandDao.lagreBosattUtland(bosattUtland)
        val hentBosattUtland = bosattUtlandDao.hentBosattUtland(behandlingid)
        hentBosattUtland shouldBe bosattUtland
    }
}
