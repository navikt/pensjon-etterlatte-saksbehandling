package statistikk.jobs

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.statistikk.database.KjoertStatus
import no.nav.etterlatte.statistikk.domain.MaanedStatistikk
import no.nav.etterlatte.statistikk.jobs.MaanedligStatistikkJob
import no.nav.etterlatte.statistikk.service.StatistikkService
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneOffset

class MaanedligStatistikkJobTest {

    @Test
    fun `run stopper hvis den ikke er leader`() {
        val leaderElection: LeaderElection = mockk()
        every { leaderElection.isLeader() } returns false
        val statistikkService: StatistikkService = mockk()

        val sut = MaanedligStatistikkJob.ProduserOgLagreMaanedligStatistikk(
            leaderElection = leaderElection,
            statistikkService = statistikkService,
            clock = Clock.systemUTC()
        )

        sut.run()
        verify(exactly = 0) { statistikkService.statistikkProdusertForMaaned(any()) }
        verify(exactly = 0) { statistikkService.produserStoenadStatistikkForMaaned(any()) }
        verify(exactly = 0) { statistikkService.lagreMaanedsstatistikk(any()) }
    }

    @Test
    fun `ingen statistikk lagres hvis kjoertStatus for maaneden er INGEN_FEIL`() {
        val maanedProdusert = YearMonth.of(2022, 8)

        val leaderElection: LeaderElection = mockk()
        every { leaderElection.isLeader() } returns true
        val statistikkService: StatistikkService = mockk()
        every { statistikkService.statistikkProdusertForMaaned(maanedProdusert) } returns KjoertStatus.INGEN_FEIL

        val clockMaanedEtterProdusert: Clock = Clock.fixed(
            maanedProdusert.plusMonths(1)
                .atDay(1)
                .atTime(1, 1)
                .toInstant(ZoneOffset.UTC),
            norskTidssone
        )

        val sut = MaanedligStatistikkJob.ProduserOgLagreMaanedligStatistikk(
            leaderElection = leaderElection,
            statistikkService = statistikkService,
            clock = clockMaanedEtterProdusert
        )

        sut.run()
        verify(exactly = 1) { statistikkService.statistikkProdusertForMaaned(any()) }
        verify(exactly = 0) { statistikkService.produserStoenadStatistikkForMaaned(any()) }
        verify(exactly = 0) { statistikkService.lagreMaanedsstatistikk(any()) }
    }

    @Test
    fun `ingen statistikk lagres hvis kjoertStatus for maaneden er FEIL`() {
        val maanedFeil = YearMonth.of(2022, 9)

        val leaderElection: LeaderElection = mockk()
        every { leaderElection.isLeader() } returns true
        val statistikkService: StatistikkService = mockk()
        every { statistikkService.statistikkProdusertForMaaned(maanedFeil) } returns KjoertStatus.FEIL

        val clockMaanedEtterProdusert: Clock = Clock.fixed(
            maanedFeil.plusMonths(1)
                .atDay(1)
                .atTime(1, 1)
                .toInstant(ZoneOffset.UTC),
            norskTidssone
        )

        val sut = MaanedligStatistikkJob.ProduserOgLagreMaanedligStatistikk(
            leaderElection = leaderElection,
            statistikkService = statistikkService,
            clock = clockMaanedEtterProdusert
        )

        sut.run()
        verify(exactly = 1) { statistikkService.statistikkProdusertForMaaned(any()) }
        verify(exactly = 0) { statistikkService.produserStoenadStatistikkForMaaned(any()) }
        verify(exactly = 0) { statistikkService.lagreMaanedsstatistikk(any()) }
    }

    @Test
    fun `statistikk hentes og lagres hvis kjoertStatus for maaneden er IKKE_KJOERT`() {
        val maanedIkkeKjoert = YearMonth.of(2022, 9)
        val mockMaanedStatistikk: MaanedStatistikk = mockk()

        val leaderElection: LeaderElection = mockk()
        every { leaderElection.isLeader() } returns true
        val statistikkService: StatistikkService = mockk()
        every { statistikkService.statistikkProdusertForMaaned(maanedIkkeKjoert) } returns KjoertStatus.IKKE_KJOERT
        every { statistikkService.produserStoenadStatistikkForMaaned(maanedIkkeKjoert) } returns mockMaanedStatistikk
        every { statistikkService.lagreMaanedsstatistikk(mockMaanedStatistikk) } returns Unit

        val clockMaanedEtterProdusert: Clock = Clock.fixed(
            maanedIkkeKjoert.plusMonths(1)
                .atDay(1)
                .atTime(1, 1)
                .toInstant(ZoneOffset.UTC),
            norskTidssone
        )

        val sut = MaanedligStatistikkJob.ProduserOgLagreMaanedligStatistikk(
            leaderElection = leaderElection,
            statistikkService = statistikkService,
            clock = clockMaanedEtterProdusert
        )

        sut.run()
        verify(exactly = 1) { statistikkService.statistikkProdusertForMaaned(any()) }
        verify(exactly = 1) { statistikkService.produserStoenadStatistikkForMaaned(any()) }
        verify(exactly = 1) { statistikkService.lagreMaanedsstatistikk(any()) }
    }
}