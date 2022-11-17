package no.nav.etterlatte

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.statistikk.database.DataSourceBuilder
import no.nav.etterlatte.statistikk.database.SakRad
import no.nav.etterlatte.statistikk.database.SakstatistikkRepository
import no.nav.etterlatte.statistikk.database.StatistikkRepository
import no.nav.etterlatte.statistikk.database.StoenadRad
import no.nav.etterlatte.statistikk.service.VedtakHendelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DBTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

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
    fun testStatitstikkRepo() {
        val repo = StatistikkRepository.using(dataSource)
        repo.lagreStoenadsrad(
            StoenadRad(
                -1,
                "123",
                listOf("23427249697", "18458822782"),
                listOf(),
                "40",
                "1000",
                "FOLKETRYGD",
                "0,4G",
                UUID.randomUUID(),
                5,
                5,
                Instant.now(),
                "BP",
                "42",
                "Berit Behandler",
                "Arne Attestant",
                LocalDate.now(),
                null
            )
        )
        repo.datapakke().also {
            Assertions.assertEquals(1, it.size)
            val stoenadRad = it.first()
            Assertions.assertEquals(5, stoenadRad.sakId)
            Assertions.assertEquals(
                stoenadRad.fnrForeldre,
                listOf("23427249697", "18458822782")
            )
        }
    }

    @Test
    fun testSakRepo() {
        val repo = SakstatistikkRepository.using(dataSource)
        val lagretRad = repo.lagreRad(
            SakRad(
                id = -2,
                behandlingId = UUID.randomUUID(),
                sakId = 1337,
                mottattTidspunkt = Tidspunkt.now(),
                registrertTidspunkt = Tidspunkt.now(),
                ferdigbehandletTidspunkt = null,
                vedtakTidspunkt = null,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingStatus = VedtakHendelse.IVERKSATT,
                behandlingResultat = "wow",
                resultatBegrunnelse = "for en begrunnelse",
                behandlingMetode = "manuell",
                opprettetAv = "test",
                ansvarligBeslutter = "test testesen",
                aktorId = "12345678911",
                datoFoersteUtbetaling = LocalDate.now(),
                tekniskTid = Tidspunkt.now(),
                sakYtelse = "En ytelse",
                vedtakLoependeFom = LocalDate.now(),
                vedtakLoependeTom = LocalDate.now().plusYears(3)
            )
        )

        Assertions.assertEquals(repo.hentRader()[0], lagretRad)
    }
}