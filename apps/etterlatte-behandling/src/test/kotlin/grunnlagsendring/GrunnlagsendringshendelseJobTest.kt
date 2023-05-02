package no.nav.etterlatte.grunnlagsendring

import com.zaxxer.hikari.HikariDataSource
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.jobs.LeaderElection
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

internal class GrunnlagsendringshendelseJobTest {

    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService = mockk {
        every { sjekkKlareGrunnlagsendringshendelser(any()) } returns Unit
    }
    private val closed: AtomicBoolean = AtomicBoolean(false)
    private val leaderElection: LeaderElection = mockk()
    private val dataSource = mockk<HikariDataSource>()
    private val grunnlagsendringshendelseJob = GrunnlagsendringshendelseJob.SjekkKlareGrunnlagsendringshendelser(
        grunnlagsendringshendelseService = grunnlagsendringshendelseService,
        leaderElection = leaderElection,
        jobbNavn = "jobb",
        minutterGamleHendelser = 1L,
        datasource = dataSource,
        closed = closed
    )

    @Test
    fun `skal ikke utfoere jobb siden pod ikke er leader`() {
        every { leaderElection.isLeader() } returns false

        runBlocking { grunnlagsendringshendelseJob.run() }

        coVerify(exactly = 0) { grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(any()) }
        assertFalse(leaderElection.isLeader())
    }

    @Test
    fun `skal ikke utfoere jobb siden pod er i shutdown`() {
        every { leaderElection.isLeader() } returns true
        closed.set(true) // simulerer shutdown
        runBlocking { grunnlagsendringshendelseJob.run() }

        coVerify(exactly = 0) { grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(any()) }
    }

    @Test
    fun `skal utfoere jobb siden pod er leader`() {
        every { leaderElection.isLeader() } returns true

        runBlocking { grunnlagsendringshendelseJob.run() }

        coVerify(exactly = 1) { grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(any()) }
        assertTrue(leaderElection.isLeader())
    }
}