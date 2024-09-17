package grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelserKafkaService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Status
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Utfall
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarDNummer
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarUtvandret
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedLeverIPDL
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt.BehandleDoedshendelseKontrollpunktService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellom18og20PaaReformtidspunkt.BehandleDoedshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.MellomAttenOgTjueVedReformtidspunktFeatureToggle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.person.krr.KrrKlientImpl
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BehandleDoedshendelseServiceTest {
    private val dao = mockk<DoedshendelseDao>()
    private val kontrollpunktService = mockk<BehandleDoedshendelseKontrollpunktService>()
    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>()
    private val sakService =
        mockk<SakService> {
            every { finnEllerOpprettSakMedGrunnlag(any(), any()) } returns
                Sak(
                    ident = "12345678901",
                    sakType = SakType.BARNEPENSJON,
                    id = 1L,
                    enhet = Enhet.AALESUND,
                )
        }
    private val doedshendelserProducer =
        mockk<DoedshendelserKafkaService> {
            every { sendBrevRequestBPMellomAttenOgTjueVedReformtidspunkt(any(), false, true) } just runs
            every { sendBrevRequestBP(any(), false, false) } just runs
        }

    private val grunnlagService =
        mockk<GrunnlagService> {
            coEvery { leggInnNyttGrunnlagSak(any(), any()) } just runs
            coEvery { leggTilNyeOpplysningerBareSak(any(), any()) } just runs
        }

    private val pdlTjenesterKlient =
        mockk<PdlTjenesterKlient> {
            every { hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns mockPerson()
        }

    private val krrKlient =
        mockk<KrrKlientImpl> {
            coEvery { hentDigitalKontaktinformasjon(any()) } returns
                DigitalKontaktinformasjon(
                    personident = "",
                    aktiv = true,
                    kanVarsles = true,
                    reservert = false,
                    spraak = "nb",
                    epostadresse = null,
                    mobiltelefonnummer = null,
                    sikkerDigitalPostkasse = null,
                )
        }
    private val service =
        BehandleDoedshendelseService(
            doedshendelseDao = dao,
            doedshendelseKontrollpunktService = kontrollpunktService,
            featureToggleService = toggle,
            grunnlagsendringshendelseService = grunnlagsendringshendelseService,
            sakService = sakService,
            doedshendelserProducer,
            grunnlagService,
            pdlTjenesterKlient,
            krrKlient = krrKlient,
        )

    @Test
    fun `skal kjoere 1 ny hendelse`() {
        val doedshendelseInternal =
            DoedshendelseInternal.nyHendelse(
                avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                avdoedDoedsdato = LocalDate.now(),
                beroertFnr = "12345678901",
                relasjon = Relasjon.BARN,
                endringstype = Endringstype.OPPRETTET,
            )

        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns listOf(AvdoedHarDNummer)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns UUID.randomUUID()
            }

        service.haandterDoedshendelse(doedshendelseInternal)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(any()) }
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) }
    }

    @Test
    fun `skal avbryte hendelse hvis avdoed er ikke er registert doed i PDL`() {
        val doedshendelseInternal =
            DoedshendelseInternal
                .nyHendelse(
                    avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                    avdoedDoedsdato = LocalDate.now(),
                    beroertFnr = "12345678901",
                    relasjon = Relasjon.BARN,
                    endringstype = Endringstype.OPPRETTET,
                )

        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns listOf(AvdoedLeverIPDL)
        val doedshendelseInternalCapture = slot<DoedshendelseInternal>()

        service.haandterDoedshendelse(doedshendelseInternal)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(capture(doedshendelseInternalCapture)) }
        verify(exactly = 0) { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) }
        doedshendelseInternalCapture.captured.status shouldBe Status.FERDIG
        doedshendelseInternalCapture.captured.utfall shouldBe Utfall.AVBRUTT
    }

    @Test
    fun `skal ferdigstille doedshendelse med status ferdig og sette oppgaveId`() {
        val doedshendelseInternal =
            DoedshendelseInternal
                .nyHendelse(
                    avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                    avdoedDoedsdato = LocalDate.now(),
                    beroertFnr = "12345678901",
                    relasjon = Relasjon.BARN,
                    endringstype = Endringstype.OPPRETTET,
                )

        every { dao.hentDoedshendelserMedStatus(any()) } returns listOf(doedshendelseInternal)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        val oppgaveId = UUID.randomUUID()
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns oppgaveId
            }
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns
            listOf(AvdoedHarUtvandret, AvdoedHarDNummer)
        val doedshendelseCapture = slot<DoedshendelseInternal>()

        service.haandterDoedshendelse(doedshendelseInternal)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(capture(doedshendelseCapture)) }
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) }
        doedshendelseCapture.captured.status shouldBe Status.FERDIG
        doedshendelseCapture.captured.utfall shouldBe Utfall.OPPGAVE
        doedshendelseCapture.captured.oppgaveId shouldBe oppgaveId
    }

    @Test
    fun `Skal sjekke sende med bor i utlandet til brev`() {
        val doedshendelseInternal =
            DoedshendelseInternal
                .nyHendelse(
                    avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                    avdoedDoedsdato = LocalDate.now(),
                    beroertFnr = "12345678901",
                    relasjon = Relasjon.BARN,
                    endringstype = Endringstype.OPPRETTET,
                )

        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        val oppgaveId = UUID.randomUUID()
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns oppgaveId
            }
        every { toggle.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanSendeBrevOgOppretteOppgave, any()) } returns true
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns
            emptyList()
        every { doedshendelserProducer.sendBrevRequestBPMellomAttenOgTjueVedReformtidspunkt(any(), any(), any()) } just runs
        val doedshendelseCapture = slot<DoedshendelseInternal>()

        service.haandterDoedshendelse(doedshendelseInternal)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(capture(doedshendelseCapture)) }
        verify { doedshendelserProducer.sendBrevRequestBPMellomAttenOgTjueVedReformtidspunkt(any(), any(), any()) }
        doedshendelseCapture.captured.status shouldBe Status.FERDIG
        doedshendelseCapture.captured.utfall shouldBe Utfall.BREV
    }

    @Test
    fun `skal ikke opprette oppgave for doedshendelse dersom feature-toggle er av`() {
        val doedshendelseInternal =
            DoedshendelseInternal
                .nyHendelse(
                    avdoedFnr = AVDOED2_FOEDSELSNUMMER.value,
                    avdoedDoedsdato = LocalDate.now(),
                    beroertFnr = "12345678901",
                    relasjon = Relasjon.BARN,
                    endringstype = Endringstype.OPPRETTET,
                )

        every { dao.hentDoedshendelserMedStatus(any()) } returns listOf(doedshendelseInternal)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        val oppgaveId = UUID.randomUUID()
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns oppgaveId
            }
        every { toggle.isEnabled(MellomAttenOgTjueVedReformtidspunktFeatureToggle.KanSendeBrevOgOppretteOppgave, any()) } returns false
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns
            listOf(AvdoedHarUtvandret, AvdoedHarDNummer)
        val doedshendelseCapture = slot<DoedshendelseInternal>()

        service.haandterDoedshendelse(doedshendelseInternal)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(capture(doedshendelseCapture)) }
        verify(exactly = 0) { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) }
        doedshendelseCapture.captured.status shouldBe Status.FERDIG
        doedshendelseCapture.captured.utfall shouldBe Utfall.OPPGAVE
        doedshendelseCapture.captured.oppgaveId shouldBe null
    }
}
