package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarDNummer
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedHarUtvandret
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunkt.AvdoedLeverIPDL
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.krr.KrrKlientImpl
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.mockDoedshendelsePerson
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class DoedshendelseJobServiceTest {
    private val dao = mockk<DoedshendelseDao>()
    private val kontrollpunktService = mockk<DoedshendelseKontrollpunktService>()
    private val grunnlagsendringshendelseService = mockk<GrunnlagsendringshendelseService>()
    private val dataSource = mockk<DataSource>()
    private val kontekst =
        Context(
            Self(this::class.java.simpleName),
            DatabaseContextTest(dataSource),
            mockk(),
            HardkodaSystembruker.testdata,
        )
    private val sakService =
        mockk<SakService> {
            every { finnEllerOpprettSakMedGrunnlag(any(), any()) } returns
                Sak(
                    ident = "12345678901",
                    sakType = SakType.BARNEPENSJON,
                    id = sakId1,
                    enhet = Enheter.AALESUND.enhetNr,
                    null,
                    false,
                )
        }
    private val femDagerGammel = 5
    private val doedshendelserProducer =
        mockk<DoedshendelserKafkaService> {
            every { sendBrevRequestOMS(any(), false) } just runs
            every { sendBrevRequestBP(any(), false, false) } just runs
        }

    private val grunnlagService =
        mockk<GrunnlagService> {
            coJustRun { opprettGrunnlag(any(), any()) }
            justRun { lagreNyeSaksopplysningerBareSak(any(), any()) }
            coJustRun { opprettEllerOppdaterGrunnlagForSak(any(), any()) }
        }

    private val pdlTjenesterKlient =
        mockk<PdlTjenesterKlient> {
            every { hentPdlModellDoedshendelseForSaktype(any(), any(), SakType.BARNEPENSJON) } returns mockDoedshendelsePerson()
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
        DoedshendelseJobService(
            doedshendelseDao = dao,
            doedshendelseKontrollpunktService = kontrollpunktService,
            grunnlagsendringshendelseService = grunnlagsendringshendelseService,
            sakService = sakService,
            dagerGamleHendelserSomSkalKjoeres = femDagerGammel,
            doedshendelserProducer,
            grunnlagService,
            pdlTjenesterKlient,
            krrKlient = krrKlient,
        )

    private val bruker = systembruker()

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
                    endret = LocalDateTime.now().minusDays(femDagerGammel.toLong()).toTidspunkt(),
                ),
            )
        every { kontrollpunktService.identifiserKontrollpunkter(any(), bruker) } returns listOf(AvdoedHarDNummer)
        every { dao.hentDoedshendelserMedStatus(any()) } returns doedshendelser
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns UUID.randomUUID()
            }

        service.setupKontekstAndRun(kontekst, bruker)

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
                ).copy(endret = LocalDateTime.now().minusDays(femDagerGammel.toLong()).toTidspunkt())

        every { dao.hentDoedshendelserMedStatus(any()) } returns listOf(doedshendelseInternal)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        every { kontrollpunktService.identifiserKontrollpunkter(any(), bruker) } returns listOf(AvdoedLeverIPDL)
        val doedshendelseInternalCapture = slot<DoedshendelseInternal>()

        service.setupKontekstAndRun(kontekst, bruker)

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
                ).copy(endret = LocalDateTime.now().minusDays(femDagerGammel.toLong()).toTidspunkt())

        every { dao.hentDoedshendelserMedStatus(any()) } returns listOf(doedshendelseInternal)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        val oppgaveId = UUID.randomUUID()
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns oppgaveId
            }
        every { kontrollpunktService.identifiserKontrollpunkter(any(), bruker) } returns
            listOf(AvdoedHarUtvandret, AvdoedHarDNummer)
        val doedshendelseCapture = slot<DoedshendelseInternal>()

        service.setupKontekstAndRun(kontekst, bruker)

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
                ).copy(endret = LocalDateTime.now().minusDays(femDagerGammel.toLong()).toTidspunkt())

        every { dao.hentDoedshendelserMedStatus(any()) } returns listOf(doedshendelseInternal)
        every { dao.oppdaterDoedshendelse(any()) } returns Unit
        val oppgaveId = UUID.randomUUID()
        every { grunnlagsendringshendelseService.opprettDoedshendelseForPerson(any()) } returns
            mockk {
                every { id } returns oppgaveId
            }
        every { kontrollpunktService.identifiserKontrollpunkter(any(), bruker) } returns
            emptyList()
        every { doedshendelserProducer.sendBrevRequestBP(any(), any(), any()) } just runs
        val doedshendelseCapture = slot<DoedshendelseInternal>()

        service.setupKontekstAndRun(kontekst, bruker)

        verify(exactly = 1) { dao.oppdaterDoedshendelse(capture(doedshendelseCapture)) }
        verify { doedshendelserProducer.sendBrevRequestBP(any(), any(), any()) }
        doedshendelseCapture.captured.status shouldBe Status.FERDIG
        doedshendelseCapture.captured.utfall shouldBe Utfall.BREV
    }
}
