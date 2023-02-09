package no.nav.etterlatte

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.statistikk.database.DataSourceBuilder
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.service.VedtakHendelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
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

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )

        dataSource.migrate(gcp = false)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun testStatitstikkRepo() {
        val repo = StoenadRepository.using(dataSource)
        repo.lagreStoenadsrad(
            StoenadRad(
                id = -1,
                fnrSoeker = "123",
                fnrForeldre = listOf("23427249697", "18458822782"),
                fnrSoesken = listOf(),
                anvendtTrygdetid = "40",
                nettoYtelse = "1000",
                beregningType = "FOLKETRYGD",
                anvendtSats = "0,4G",
                behandlingId = UUID.randomUUID(),
                sakId = 5,
                sakNummer = 5,
                tekniskTid = Tidspunkt.now(),
                sakYtelse = "BP",
                versjon = "42",
                saksbehandler = "Berit Behandler",
                attestant = "Arne Attestant",
                vedtakLoependeFom = LocalDate.now(),
                vedtakLoependeTom = null,
                beregning = null
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
        val repo = SakRepository.using(dataSource)
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
                behandlingStatus = VedtakHendelse.IVERKSATT.name,
                behandlingResultat = null,
                resultatBegrunnelse = "for en begrunnelse",
                behandlingMetode = BehandlingMetode.MANUELL,
                opprettetAv = "test",
                ansvarligBeslutter = "test testesen",
                aktorId = "12345678911",
                datoFoersteUtbetaling = LocalDate.now(),
                tekniskTid = Tidspunkt.now(),
                sakYtelse = "En ytelse",
                vedtakLoependeFom = LocalDate.now(),
                vedtakLoependeTom = LocalDate.now().plusYears(3),
                saksbehandler = "en saksbehandler",
                ansvarligEnhet = "en enhet",
                soeknadFormat = null,
                sakUtland = SakUtland.NASJONAL,
                beregning = null
            )
        )

        Assertions.assertEquals(repo.hentRader()[0], lagretRad)
    }
}