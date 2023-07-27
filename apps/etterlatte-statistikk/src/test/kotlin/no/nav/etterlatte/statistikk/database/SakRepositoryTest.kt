package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.Beregningstype
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.service.VedtakHendelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
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
class SakRepositoryTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

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

    val mockBeregning = Beregning(
        beregningId = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        type = Beregningstype.BP,
        beregnetDato = Tidspunkt.now(),
        beregningsperioder = listOf()
    )

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE TABLE sak")
                .executeUpdate()
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
                beregning = mockBeregning,
                sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
                avdoedeForeldre = emptyList(),
                revurderingAarsak = "MIGRERING"
            )
        )

        Assertions.assertEquals(repo.hentRader()[0], lagretRad)
        Assertions.assertEquals(lagretRad?.beregning, mockBeregning)
    }

    @Test
    fun `sakRepository lagrer ned og henter ut null for beregning riktig`() {
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
                beregning = null,
                sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
                avdoedeForeldre = emptyList(),
                revurderingAarsak = "MIGRERING"
            )
        )
        Assertions.assertNotNull(lagretRad)
        Assertions.assertNull(lagretRad?.beregning)
        Assertions.assertNull(repo.hentRader()[0].beregning)
    }
}