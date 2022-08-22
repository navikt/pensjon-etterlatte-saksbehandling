package no.nav.etterlatte.grunnlagsendring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.common.LeaderElection
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GrunnlagsendringshendelseJobTest {

    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService = mockk() {
        every { sjekkKlareGrunnlagsendringshendelser(any()) } returns Unit
    }
    private val leaderElection: LeaderElection = mockk()
    private val grunnlagsendringshendelseJob = GrunnlagsendringshendelseJob.SjekkKlareGrunnlagsendringshendelser(
        grunnlagsendringshendelseService = grunnlagsendringshendelseService,
        leaderElection = leaderElection,
        jobbNavn = "jobb",
        minutterGamleHendelser = 1L
    )

    @Test
    fun `skal ikke utfoere jobb siden pod ikke er leader`() {
        every { leaderElection.isLeader() } returns false

        grunnlagsendringshendelseJob.run()

        verify(exactly = 0) { grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(any()) }
        assertFalse(leaderElection.isLeader())
    }

    @Test
    fun `skal utfoere jobb siden pod er leader`() {
        every { leaderElection.isLeader() } returns true
        every { grunnlagsendringshendelseService.sjekkKlareDoedshendelser(any()) } returns Unit

        grunnlagsendringshendelseJob.run()

        verify(exactly = 1) { grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(any()) }
        assertTrue(leaderElection.isLeader())
    }
}