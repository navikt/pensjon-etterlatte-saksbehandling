package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.Beregningstype
import no.nav.etterlatte.statistikk.domain.MaanedStoenadRad
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.domain.stoenadRad
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoenadRepositoryTest {

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

    val mockBeregning = Beregning(
        beregningId = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        type = Beregningstype.BP,
        beregnetDato = Tidspunkt(instant = Instant.now()),
        beregningsperioder = listOf()
    )

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE TABLE stoenad")
                .executeUpdate()
        }
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE TABLE maanedsstatistikk_job")
                .executeUpdate()
        }
    }

    @Test
    fun testStoenadRepository() {
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
                anvendtSats = "1G",
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
                beregning = mockBeregning,
                vedtakType = VedtakType.INNVILGELSE
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
            Assertions.assertEquals(stoenadRad.beregning, mockBeregning)
            Assertions.assertEquals(stoenadRad.vedtakType, VedtakType.INNVILGELSE)
        }
    }

    @Test
    fun `datapakke henter ut null for beregning riktig`() {
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
                anvendtSats = "1G",
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
                beregning = null,
                vedtakType = VedtakType.INNVILGELSE
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
            Assertions.assertNull(stoenadRad.beregning)
            Assertions.assertEquals(stoenadRad.vedtakType, VedtakType.INNVILGELSE)
        }
    }

    @Test
    fun `hentRaderInnenforMaaned ignorerer rader som ikke er aktive innenfor maaneden`() {
        val repo = StoenadRepository.using(dataSource)
        val maaned = YearMonth.of(2022, 8)
        val tekniskTid = LocalDate.of(2022, 5, 1).atStartOfDay().toNorskTidspunkt()

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 1L,
                vedtakLoependeFom = maaned.plusMonths(1).atDay(1),
                vedtakLoependeTom = null,
                tekniskTid = tekniskTid
            )
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 2L,
                vedtakLoependeFom = maaned.minusMonths(5).atDay(1),
                vedtakLoependeTom = maaned.minusMonths(1).atEndOfMonth(),
                tekniskTid = tekniskTid
            )
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 3L,
                vedtakLoependeFom = maaned.atDay(1),
                tekniskTid = tekniskTid
            )
        )

        val maanedsrader = repo.hentRaderInnenforMaaned(maaned)
        Assertions.assertEquals(maanedsrader.size, 1)
        Assertions.assertEquals(maanedsrader[0].sakId, 3L)
    }

    @Test
    fun `hentRaderInnenforMaaned ignorerer rader som er fattet etter maaneden`() {
        val repo = StoenadRepository.using(dataSource)
        val maaned = YearMonth.of(2022, 8)
        val tekniskTidEtterMaaned = maaned.plusMonths(1).atDay(1).atStartOfDay().toNorskTidspunkt()
        val tekniskTidInnenforMaaned = maaned.atDay(1).atStartOfDay().toNorskTidspunkt()
        val tekniskTidFoerMaaned = maaned.minusMonths(1).atDay(1).atStartOfDay().toNorskTidspunkt()

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 1L,
                vedtakLoependeFom = maaned.atDay(1),
                vedtakLoependeTom = null,
                tekniskTid = tekniskTidFoerMaaned
            )
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 2L,
                vedtakLoependeFom = maaned.minusMonths(5).atDay(1),
                tekniskTid = tekniskTidInnenforMaaned
            )
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 3L,
                vedtakLoependeFom = maaned.atDay(1),
                tekniskTid = tekniskTidEtterMaaned
            )
        )

        val maanedsrader = repo.hentRaderInnenforMaaned(maaned)
        Assertions.assertEquals(maanedsrader.size, 2)
        Assertions.assertEquals(maanedsrader.map { it.sakId }.toSet(), setOf(1L, 2L))
    }

    @Test
    fun `hentRaderInnenforMaaned gir alle rader som har et vedtak løpende innenfor måneden`() {
        val repo = StoenadRepository.using(dataSource)
        val maaned = YearMonth.of(2022, 8)
        val tekniskTidFoerMaaned = maaned.minusMonths(1).atDay(1).atStartOfDay().toNorskTidspunkt()

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 1L,
                vedtakLoependeFom = maaned.minusMonths(5).atDay(1),
                tekniskTid = tekniskTidFoerMaaned
            )
        )
        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 1L,
                vedtakLoependeFom = maaned.atDay(1),
                vedtakLoependeTom = null,
                tekniskTid = tekniskTidFoerMaaned
            )
        )
        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = 1L,
                vedtakLoependeFom = maaned.atDay(1),
                tekniskTid = tekniskTidFoerMaaned
            )
        )

        val maanedsrader = repo.hentRaderInnenforMaaned(maaned)
        Assertions.assertEquals(maanedsrader.size, 3)
        Assertions.assertEquals(maanedsrader.map { it.sakId }.toSet(), setOf(1L))
    }

    @Test
    fun `maanedsstatistikk lagrer ned en rad riktig`() {
        val repo = StoenadRepository.using(dataSource)
        val maanedStoenadRad = MaanedStoenadRad(
            id = -1,
            fnrSoeker = "123",
            fnrForeldre = listOf("321"),
            fnrSoesken = listOf(),
            anvendtTrygdetid = "40",
            nettoYtelse = "4",
            beregningType = "Moro",
            anvendtSats = "1",
            behandlingId = UUID.randomUUID(),
            sakId = 0,
            sakNummer = 0,
            tekniskTid = Tidspunkt.now(),
            sakYtelse = "",
            versjon = "",
            saksbehandler = "",
            attestant = null,
            vedtakLoependeFom = LocalDate.of(2022, 8, 1),
            vedtakLoependeTom = null,
            statistikkMaaned = YearMonth.of(2023, Month.FEBRUARY)
        )

        repo.lagreMaanedStatistikkRad(maanedStoenadRad)
    }

    @Test
    fun `stoenadRepository lagrer ned og henter ut null for beregning riktig`() {
        val repo = StoenadRepository.using(dataSource)
        val lagretRad = repo.lagreStoenadsrad(
            StoenadRad(
                id = -1,
                fnrSoeker = "123",
                fnrForeldre = listOf("23427249697", "18458822782"),
                fnrSoesken = listOf(),
                anvendtTrygdetid = "40",
                nettoYtelse = "1000",
                beregningType = "FOLKETRYGD",
                anvendtSats = "1G",
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
                beregning = null,
                vedtakType = null
            )
        )

        Assertions.assertNotNull(lagretRad)
        Assertions.assertNull(lagretRad?.beregning)
        Assertions.assertNull(repo.datapakke()[0].beregning)
        Assertions.assertNull(repo.datapakke()[0].vedtakType)
    }

    @Test
    fun `lagreMaanedJobUtfoert lagrer ned kjøring for en maaned riktig`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 0,
            raderRegistrert = 20
        )

        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned = maaned)
        Assertions.assertEquals(KjoertStatus.INGEN_FEIL, kjoertStatus)
    }

    @Test
    fun `kjoertStatusForMaanedsstatistikk henter ut med preferanse INGEN_FEIL`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 7,
            raderRegistrert = 20
        )

        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 0,
            raderRegistrert = 20
        )

        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned = maaned)
        Assertions.assertEquals(KjoertStatus.INGEN_FEIL, kjoertStatus)
    }

    @Test
    fun `kjoertStatusForMaanedsstatistikk gir status FEIL hvis kun feil er registrert for maaned`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 1,
            raderRegistrert = 20
        )
        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned)
        Assertions.assertEquals(KjoertStatus.FEIL, kjoertStatus)
    }

    @Test
    fun `kjoertStatusForMaanedsstatistikk gir IKKE_KJOERT hvis ingen registrering finnes for måned`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 1,
            raderRegistrert = 20
        )
        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned.plusMonths(1))
        Assertions.assertEquals(KjoertStatus.IKKE_KJOERT, kjoertStatus)
    }
}