package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.domain.AvkortetYtelse
import no.nav.etterlatte.statistikk.domain.Avkorting
import no.nav.etterlatte.statistikk.domain.AvkortingGrunnlag
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.Beregningstype
import no.nav.etterlatte.statistikk.domain.MaanedStoenadRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.domain.StoenadPeriodeType
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.domain.StoenadUtbetalingsperiode
import no.nav.etterlatte.statistikk.domain.stoenadRad
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.RegisterExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoenadRepositoryTest(
    private val dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    val mockBeregning =
        Beregning(
            beregningId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            type = Beregningstype.BP,
            beregnetDato = Tidspunkt.now(),
            beregningsperioder = emptyList(),
        )

    val mockAvkorting =
        Avkorting(
            listOf(
                AvkortingGrunnlag(
                    fom = YearMonth.now(),
                    tom = null,
                    aarsinntekt = 100,
                    fratrekkInnAar = 40,
                    relevanteMaanederInnAar = 2,
                    spesifikasjon = "",
                ),
            ),
            listOf(
                AvkortetYtelse(
                    fom = YearMonth.now(),
                    tom = null,
                    ytelseFoerAvkorting = 200,
                    avkortingsbeloep = 50,
                    ytelseEtterAvkorting = 150,
                    restanse = 0,
                    sanksjonertYtelse = null,
                ),
            ),
        )

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun `lagreStoenadsrad lagrer alle felter til`() {
        val repo = StoenadRepository.using(dataSource)
        val sak5 = SakId(5)
        repo.lagreStoenadsrad(
            StoenadRad(
                id = -1,
                fnrSoeker = "123",
                fnrForeldre = listOf("23427249697", "18458822782"),
                fnrSoesken = emptyList(),
                anvendtTrygdetid = "40",
                nettoYtelse = "1000",
                beregningType = "FOLKETRYGD",
                anvendtSats = "1G",
                behandlingId = UUID.randomUUID(),
                sakId = sak5,
                sakNummer = 5,
                tekniskTid = Tidspunkt.now(),
                sakYtelse = "BP",
                versjon = "42",
                saksbehandler = "Berit Behandler",
                attestant = "Arne Attestant",
                vedtakLoependeFom = LocalDate.now(),
                vedtakLoependeTom = null,
                beregning = mockBeregning,
                avkorting = mockAvkorting,
                vedtakType = VedtakType.INNVILGELSE,
                sakUtland = SakUtland.NASJONAL,
                virkningstidspunkt = YearMonth.of(2023, 6),
                utbetalingsdato = LocalDate.of(2023, 7, 20),
                vedtaksloesning = Vedtaksloesning.GJENNY,
                pesysId = 123L,
                sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
                opphoerFom = YearMonth.of(2024, 1),
                vedtaksperioder =
                    listOf(
                        StoenadUtbetalingsperiode(
                            type = StoenadPeriodeType.UTBETALING,
                            beloep = BigDecimal(1000),
                            fraOgMed = YearMonth.of(2023, 6),
                            tilOgMed = YearMonth.of(2023, 12),
                        ),
                        StoenadUtbetalingsperiode(
                            type = StoenadPeriodeType.UTBETALING,
                            beloep = null,
                            fraOgMed = YearMonth.of(2024, 1),
                            tilOgMed = null,
                        ),
                    ),
            ),
        )
        repo.hentStoenadRader().also {
            assertEquals(1, it.size)
            val stoenadRad = it.first()
            assertEquals(sak5, stoenadRad.sakId)
            assertEquals(
                stoenadRad.fnrForeldre,
                listOf("23427249697", "18458822782"),
            )
            assertEquals(stoenadRad.beregning, mockBeregning)
            assertEquals(stoenadRad.avkorting, mockAvkorting)
            assertEquals(stoenadRad.vedtakType, VedtakType.INNVILGELSE)
            assertEquals(stoenadRad.sakUtland, SakUtland.NASJONAL)
            assertEquals(2, stoenadRad.vedtaksperioder?.size)
            assertEquals(YearMonth.of(2024, 1), stoenadRad.opphoerFom)
        }
    }

    @Test
    fun `hentStoenadRader henter ut null for beregning riktig`() {
        val repo = StoenadRepository.using(dataSource)
        val sak5 = SakId(5)
        repo.lagreStoenadsrad(
            StoenadRad(
                id = -1,
                fnrSoeker = "123",
                fnrForeldre = listOf("23427249697", "18458822782"),
                fnrSoesken = emptyList(),
                anvendtTrygdetid = "40",
                nettoYtelse = "1000",
                beregningType = "FOLKETRYGD",
                anvendtSats = "1G",
                behandlingId = UUID.randomUUID(),
                sakId = sak5,
                sakNummer = 5,
                tekniskTid = Tidspunkt.now(),
                sakYtelse = "BP",
                versjon = "42",
                saksbehandler = "Berit Behandler",
                attestant = "Arne Attestant",
                vedtakLoependeFom = LocalDate.now(),
                vedtakLoependeTom = null,
                beregning = null,
                avkorting = null,
                vedtakType = VedtakType.INNVILGELSE,
                sakUtland = SakUtland.NASJONAL,
                virkningstidspunkt = YearMonth.of(2023, 6),
                utbetalingsdato = LocalDate.of(2023, 7, 20),
                vedtaksloesning = Vedtaksloesning.GJENNY,
                pesysId = 123L,
                sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
                opphoerFom = null,
                vedtaksperioder = null,
            ),
        )
        repo.hentStoenadRader().also {
            assertEquals(1, it.size)
            val stoenadRad = it.first()
            assertEquals(sak5, stoenadRad.sakId)
            assertEquals(
                stoenadRad.fnrForeldre,
                listOf("23427249697", "18458822782"),
            )
            assertNull(stoenadRad.beregning)
            assertNull(stoenadRad.avkorting)
            assertEquals(stoenadRad.vedtakType, VedtakType.INNVILGELSE)
        }
    }

    @Test
    fun `hentRaderInnenforMaaned ignorerer rader som ikke er aktive innenfor maaneden`() {
        val repo = StoenadRepository.using(dataSource)
        val maaned = YearMonth.of(2022, 8)
        val tekniskTid = LocalDate.of(2022, 5, 1).atStartOfDay().toNorskTidspunkt()

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId1,
                vedtakLoependeFom = maaned.plusMonths(1).atDay(1),
                vedtakLoependeTom = null,
                tekniskTid = tekniskTid,
            ),
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId2,
                vedtakLoependeFom = maaned.minusMonths(5).atDay(1),
                vedtakLoependeTom = maaned.minusMonths(1).atEndOfMonth(),
                tekniskTid = tekniskTid,
            ),
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId3,
                vedtakLoependeFom = maaned.atDay(1),
                tekniskTid = tekniskTid,
            ),
        )

        val maanedsrader = repo.hentStoenadRaderInnenforMaaned(maaned)
        assertEquals(maanedsrader.size, 1)
        assertEquals(maanedsrader[0].sakId, sakId3)
    }

    @Test
    fun `hentRaderInnenforMaaned ignorerer rader som er fattet etter maaneden`() {
        val repo = StoenadRepository.using(dataSource)
        val maaned = YearMonth.of(2022, 8)
        val tekniskTidEtterMaaned =
            maaned
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .toNorskTidspunkt()
        val tekniskTidInnenforMaaned = maaned.atDay(1).atStartOfDay().toNorskTidspunkt()
        val tekniskTidFoerMaaned =
            maaned
                .minusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .toNorskTidspunkt()

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId1,
                vedtakLoependeFom = maaned.atDay(1),
                vedtakLoependeTom = null,
                tekniskTid = tekniskTidFoerMaaned,
            ),
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId2,
                vedtakLoependeFom = maaned.minusMonths(5).atDay(1),
                tekniskTid = tekniskTidInnenforMaaned,
            ),
        )

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId3,
                vedtakLoependeFom = maaned.atDay(1),
                tekniskTid = tekniskTidEtterMaaned,
            ),
        )

        val maanedsrader = repo.hentStoenadRaderInnenforMaaned(maaned)
        assertEquals(maanedsrader.size, 2)
        assertEquals(maanedsrader.map { it.sakId }.toSet(), setOf(sakId1, sakId2))
    }

    @Test
    fun `hentRaderInnenforMaaned gir alle rader som har et vedtak løpende innenfor måneden`() {
        val repo = StoenadRepository.using(dataSource)
        val maaned = YearMonth.of(2022, 8)
        val tekniskTidFoerMaaned =
            maaned
                .minusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .toNorskTidspunkt()

        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId1,
                vedtakLoependeFom = maaned.minusMonths(5).atDay(1),
                tekniskTid = tekniskTidFoerMaaned,
            ),
        )
        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId1,
                vedtakLoependeFom = maaned.atDay(1),
                vedtakLoependeTom = null,
                tekniskTid = tekniskTidFoerMaaned,
            ),
        )
        repo.lagreStoenadsrad(
            stoenadRad(
                sakId = sakId1,
                vedtakLoependeFom = maaned.atDay(1),
                tekniskTid = tekniskTidFoerMaaned,
            ),
        )

        val maanedsrader = repo.hentStoenadRaderInnenforMaaned(maaned)
        assertEquals(maanedsrader.size, 3)
        assertEquals(maanedsrader.map { it.sakId }.toSet(), setOf(sakId1))
    }

    @Test
    fun `maanedsstatistikk lagrer ned en rad riktig`() {
        val repo = StoenadRepository.using(dataSource)
        val maanedStoenadRad =
            MaanedStoenadRad(
                id = -1,
                fnrSoeker = "123",
                fnrForeldre = listOf("321"),
                fnrSoesken = emptyList(),
                anvendtTrygdetid = "40",
                nettoYtelse = "4",
                avkortingsbeloep = "2",
                aarsinntekt = "10",
                beregningType = "Moro",
                anvendtSats = "1",
                behandlingId = UUID.randomUUID(),
                sakId = randomSakId(),
                sakNummer = 0,
                tekniskTid = Tidspunkt.now(),
                sakYtelse = "",
                versjon = "",
                saksbehandler = "",
                attestant = null,
                vedtakLoependeFom = LocalDate.of(2022, 8, 1),
                vedtakLoependeTom = null,
                statistikkMaaned = YearMonth.of(2023, Month.FEBRUARY),
                sakUtland = SakUtland.NASJONAL,
                virkningstidspunkt = YearMonth.of(2023, 6),
                utbetalingsdato = LocalDate.of(2023, 7, 20),
                kilde = Vedtaksloesning.GJENNY,
                pesysId = 123L,
                sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
                harAktivitetsplikt = "JA",
                oppfyllerAktivitet = true,
                aktivitet = "100_PROSENT_ARBEIDSSOEKER",
                sanksjon = null,
                etteroppgjoerAar = null,
                etteroppgjoerUtbetalt = null,
                etteroppgjoerNyStoenad = null,
                etteroppgjoerDifferanse = null,
                etteroppgjoerResultat = null,
                etterbetaltBeloep = null,
                tilbakekrevdBeloep = null,
            )

        assertDoesNotThrow {
            repo.lagreMaanedStatistikkRad(maanedStoenadRad)
        }
    }

    @Test
    fun `stoenadRepository lagrer ned og henter ut null for beregning riktig`() {
        val repo = StoenadRepository.using(dataSource)
        val lagretRad =
            repo.lagreStoenadsrad(
                StoenadRad(
                    id = -1,
                    fnrSoeker = "123",
                    fnrForeldre = listOf("23427249697", "18458822782"),
                    fnrSoesken = emptyList(),
                    anvendtTrygdetid = "40",
                    nettoYtelse = "1000",
                    beregningType = "FOLKETRYGD",
                    anvendtSats = "1G",
                    behandlingId = UUID.randomUUID(),
                    sakId = SakId(5),
                    sakNummer = 5,
                    tekniskTid = Tidspunkt.now(),
                    sakYtelse = "BP",
                    versjon = "42",
                    saksbehandler = "Berit Behandler",
                    attestant = "Arne Attestant",
                    vedtakLoependeFom = LocalDate.now(),
                    vedtakLoependeTom = null,
                    beregning = null,
                    avkorting = null,
                    vedtakType = null,
                    sakUtland = SakUtland.NASJONAL,
                    virkningstidspunkt = YearMonth.of(2023, 6),
                    utbetalingsdato = LocalDate.of(2023, 7, 20),
                    vedtaksloesning = Vedtaksloesning.GJENNY,
                    pesysId = 123L,
                    sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
                    opphoerFom = null,
                    vedtaksperioder = null,
                ),
            )

        assertNotNull(lagretRad)
        assertNull(lagretRad?.beregning)
        assertNull(repo.hentStoenadRader()[0].beregning)
        assertNull(repo.hentStoenadRader()[0].vedtakType)

        val nyLagretRad = repo.lagreStoenadsrad(lagretRad!!.copy(nettoYtelse = null))
        assertNull(nyLagretRad!!.nettoYtelse)
    }

    @Test
    fun `lagreMaanedJobUtfoert lagrer ned kjøring for en maaned riktig`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 0,
            raderRegistrert = 20,
        )

        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned = maaned)
        assertEquals(KjoertStatus.INGEN_FEIL, kjoertStatus)
    }

    @Test
    fun `kjoertStatusForMaanedsstatistikk henter ut med preferanse INGEN_FEIL`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 7,
            raderRegistrert = 20,
        )

        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 0,
            raderRegistrert = 20,
        )

        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned = maaned)
        assertEquals(KjoertStatus.INGEN_FEIL, kjoertStatus)
    }

    @Test
    fun `kjoertStatusForMaanedsstatistikk gir status FEIL hvis kun feil er registrert for maaned`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 1,
            raderRegistrert = 20,
        )
        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned)
        assertEquals(KjoertStatus.FEIL, kjoertStatus)
    }

    @Test
    fun `kjoertStatusForMaanedsstatistikk gir IKKE_KJOERT hvis ingen registrering finnes for måned`() {
        val maaned = YearMonth.of(2022, 8)
        val repo = StoenadRepository.using(dataSource)
        repo.lagreMaanedJobUtfoert(
            maaned = maaned,
            raderMedFeil = 1,
            raderRegistrert = 20,
        )
        val kjoertStatus = repo.kjoertStatusForMaanedsstatistikk(maaned.plusMonths(1))
        assertEquals(KjoertStatus.IKKE_KJOERT, kjoertStatus)
    }
}
