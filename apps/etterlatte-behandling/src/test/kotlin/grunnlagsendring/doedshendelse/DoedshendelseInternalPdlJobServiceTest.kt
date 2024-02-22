package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.Self
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarDNummer
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarUtvandret
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedLeverIPDL
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoedshendelseInternalPdlJobServiceTest {
    private val dao = mockk<DoedshendelseDao>()
    private val kontrollpunktService = mockk<DoedshendelseKontrollpunktService>()
    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>()
    private val dataSource = mockk<DataSource>()
    private val kontekst = Context(Self(this::class.java.simpleName), DatabaseContextTest(dataSource))
    private val todagergammel = 2
    private val service =
        DoedshendelseJobService(
            dao,
            kontrollpunktService,
            toggle,
            grunnlagsendringshendelseService,
            todagergammel,
        )

    @AfterEach
    fun afterEach() {
        clearMocks(dao, kontrollpunktService, grunnlagsendringshendelseService)
    }

    @Test
    fun `skal kjoere 1 ny gyldig hendelse som er 2 dager gammel og droppe 1`() {
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            )
        val doedshendelser =
            listOf(
                doedshendelseInternal,
                doedshendelseInternal.copy(
                    avdoedFnr = AVDOED_FOEDSELSNUMMER.value,
                    endret = LocalDateTime.now().minusDays(todagergammel.toLong()).toTidspunkt(),
                ),
            )
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns emptyList()
        every { dao.hentDoedshendelserMedStatus(any()) } returns doedshendelser
        every { grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(any(), any()) } returns emptyList()

        service.setupKontekstAndRun(kontekst)

        verify(exactly = 1) { grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(any(), any()) }
    }

    @Test
    fun `skal avbryte hendelse hvis avdoed er ikke er registert doed i PDL`() {
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            ).copy(endret = LocalDateTime.now().minusDays(todagergammel.toLong()).toTidspunkt())

        every { dao.hentDoedshendelserMedStatus(any()) } returns listOf(doedshendelseInternal)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns listOf(AvdoedLeverIPDL)
        val doedshendelseInternalCapture = slot<DoedshendelseInternal>()

        service.setupKontekstAndRun(kontekst)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(capture(doedshendelseInternalCapture)) }
        verify(exactly = 0) { grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(any(), any()) }
        doedshendelseInternalCapture.captured.status shouldBe Status.FERDIG
        doedshendelseInternalCapture.captured.utfall shouldBe Utfall.AVBRUTT
    }

    @Test
    fun `skal ikke avbryte gyldige hendelser dersom kontrollpunktene skal foere til oppgave`() {
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            ).copy(endret = LocalDateTime.now().minusDays(todagergammel.toLong()).toTidspunkt())

        every { dao.hentDoedshendelserMedStatus(any()) } returns listOf(doedshendelseInternal)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns
            listOf(AvdoedHarUtvandret, AvdoedHarDNummer)
        every { grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(any(), any()) } returns emptyList()

        service.setupKontekstAndRun(kontekst)

        verify(exactly = 0) { dao.oppdaterDoedshendelse(any()) }
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(any(), any()) }
    }
}
