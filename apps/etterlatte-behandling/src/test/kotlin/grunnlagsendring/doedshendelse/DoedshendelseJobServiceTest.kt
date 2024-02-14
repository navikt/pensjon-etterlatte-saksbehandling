package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoedshendelseJobServiceTest {
    private val dao = mockk<DoedshendelseDao>()
    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>()
    private val service = DoedshendelseJobService(dao, toggle, grunnlagsendringshendelseService)

    @Test
    fun `skal kjøre 1 ny gyldig hendelse som er 2 dager gammel og droppe 1`() {
        val doedshendelse =
            Doedshendelse.nyHendelse(
                avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
            )
        val doedshendelser =
            listOf(
                doedshendelse,
                doedshendelse.copy(avdoedFnr = AVDOED_FOEDSELSNUMMER.value, endret = LocalDateTime.now().minusDays(2L).toTidspunkt()),
            )
        every { dao.hentDoedshendelserMedStatus(any()) } returns doedshendelser
        every { grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(any(), any()) } returns emptyList()
        service.run()

        verify(exactly = 1) { grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(any(), any()) }
    }
}
