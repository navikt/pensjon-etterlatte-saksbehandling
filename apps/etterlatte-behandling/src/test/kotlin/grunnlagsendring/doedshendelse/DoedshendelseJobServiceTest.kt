package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarDNummer
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarUtvandret
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedLeverIPDL
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.migrering.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.migrering.person.krr.KrrKlient
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoedshendelseJobServiceTest {
    private val dao = mockk<DoedshendelseDao>()
    private val kontrollpunktService = mockk<DoedshendelseKontrollpunktService>()
    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>()
    private val dataSource = mockk<DataSource>()
    private val kontekst = Context(Self(this::class.java.simpleName), DatabaseContextTest(dataSource))
    private val sakService =
        mockk<SakService> {
            every { finnEllerOpprettSak(any(), any()) } returns
                Sak(
                    ident = "12345678901",
                    sakType = SakType.BARNEPENSJON,
                    id = 1L,
                    enhet = "0000",
                )
        }
    private val todagergammel = 2
    private val doedshendelserProducer =
        mockk<DoedshendelserKafkaService> {
            every { sendBrevRequest(any()) } just runs
        }

    private val grunnlagService =
        mockk<GrunnlagService> {
            every { leggInnNyttGrunnlagSak(any(), any()) } just runs
            every { leggTilNyeOpplysningerBareSak(any(), any()) } just runs
        }

    val pdlTjenesterKlient =
        mockk<PdlTjenesterKlient> {
            every { hentPdlModell(any(), any(), any()) } returns mockPerson()
        }

    val krrKlient =
        mockk<KrrKlient> {
            coEvery { hentDigitalKontaktinformasjon(any()) } returns
                DigitalKontaktinformasjon(
                    personident = "",
                    aktiv = true,
                    kanVarsles = true,
                    reservert = false,
                    spraak = "no",
                    epostadresse = null,
                    mobiltelefonnummer = null,
                    sikkerDigitalPostkasse = null,
                )
        }
    private val service =
        DoedshendelseJobService(
            doedshendelseDao = dao,
            doedshendelseKontrollpunktService = kontrollpunktService,
            featureToggleService = toggle,
            grunnlagsendringshendelseService = grunnlagsendringshendelseService,
            sakService = sakService,
            dagerGamleHendelserSomSkalKjoeres = todagergammel,
            doedshendelserProducer,
            grunnlagService,
            pdlTjenesterKlient,
            krrKlient = krrKlient,
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
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns listOf(AvdoedHarDNummer)
        every { dao.hentDoedshendelserMedStatus(any()) } returns doedshendelser
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns UUID.randomUUID()
            }

        service.setupKontekstAndRun(kontekst)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(any()) }
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) }
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
        verify(exactly = 0) { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) }
        doedshendelseInternalCapture.captured.status shouldBe Status.FERDIG
        doedshendelseInternalCapture.captured.utfall shouldBe Utfall.AVBRUTT
    }

    @Test
    fun `skal ferdigstille doedshendelse med status ferdig og sette oppgaveId`() {
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
        val oppgaveId = UUID.randomUUID()
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns oppgaveId
            }
        every { kontrollpunktService.identifiserKontrollerpunkter(any()) } returns
            listOf(AvdoedHarUtvandret, AvdoedHarDNummer)
        val doedshendelseCapture = slot<DoedshendelseInternal>()

        service.setupKontekstAndRun(kontekst)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(capture(doedshendelseCapture)) }
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) }
        doedshendelseCapture.captured.status shouldBe Status.FERDIG
        doedshendelseCapture.captured.utfall shouldBe Utfall.OPPGAVE
        doedshendelseCapture.captured.oppgaveId shouldBe oppgaveId
    }
}
