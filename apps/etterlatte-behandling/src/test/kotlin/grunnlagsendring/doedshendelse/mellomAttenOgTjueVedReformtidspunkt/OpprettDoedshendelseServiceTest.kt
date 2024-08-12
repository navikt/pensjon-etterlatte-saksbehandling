package grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.grunnlagsendring.doedshendelse.Relasjon
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.OpprettDoedshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.OpprettDoedshendelseService.Companion.mellom18og20PaaReformtidspunkt
import no.nav.etterlatte.grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt.OpprettDoedshendelseService.Companion.merEnnEller18PaaVirkningstidspunkt
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.personOpplysning
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettDoedshendelseServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val dao = mockk<DoedshendelseDao>()

    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val service =
        OpprettDoedshendelseService(
            pdlTjenesterKlient = pdlTjenesterKlient,
            doedshendelseDao = dao,
            featureToggleService = toggle,
        )

    private val avdoed =
        mockPerson().copy(
            doedsdato = OpplysningDTO(LocalDate.of(2023, 1, 1), null),
            avdoedesBarn =
                listOf(
                    personOpplysning(foedselsdato = LocalDate.of(2003, 12, 31)),
                    personOpplysning(foedselsdato = LocalDate.of(2004, 1, 1)),
                    personOpplysning(foedselsdato = LocalDate.of(2005, 6, 1)),
                    personOpplysning(foedselsdato = LocalDate.of(2005, 12, 31)),
                    personOpplysning(foedselsdato = LocalDate.of(2006, 1, 1)),
                ),
        )

    @BeforeEach
    fun beforeAll() {
        nyKontekstMedBruker(mockk())

        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()
    }

    @Test
    fun `Skal ikke opprette doedshendelse for avdød som ikke har barn`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns avdoed.copy(avdoedesBarn = null)
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 0) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal ikke opprette doedshendelse naar barnet er mer enn 20 paa reformtidspunkt`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                doedsdato = OpplysningDTO(LocalDate.of(2021, 12, 1), null),
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2003, 12, 31)),
                    ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 0) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal ikke opprette doedshendelse naar barnet er yngre enn 18 paa reformtidspunkt`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                doedsdato = OpplysningDTO(LocalDate.of(2023, 12, 1), null),
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2006, 1, 1)),
                    ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 0) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal ikke opprette doedshendelse naar barnet er yngre enn 18 paa virkningstidspunkt`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                doedsdato = OpplysningDTO(LocalDate.of(2022, 5, 1), null),
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2005, 1, 1)),
                    ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 0) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal opprette doedshendelse naar barnet er 18 paa virkningstidspunkt og 20 paa reformtidspunkt`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                doedsdato = OpplysningDTO(LocalDate.of(2021, 12, 1), null),
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2004, 1, 1)),
                    ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 1) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal opprette doedshendelse naar barnet er 18 paa virkningstidspunkt og 18 paa reformtidspunkt`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                doedsdato = OpplysningDTO(LocalDate.of(2023, 12, 1), null),
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2005, 12, 31)),
                    ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 1) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal opprette doedshendelse naar barnet er 20 paa virkningstidspunkt og 20 paa reformtidspunkt`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                doedsdato = OpplysningDTO(LocalDate.of(2023, 12, 1), null),
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2004, 1, 1)),
                    ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 1) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal lagre ny hendelse hvis ny beroert finnes`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2005, 1, 1)),
                    ),
            )
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 1) {
            dao.opprettDoedshendelse(any())
        }

        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(
                avdoedesBarn =
                    listOf(
                        personOpplysning(foedselsdato = LocalDate.of(2005, 1, 1)),
                        personOpplysning(foedselsdato = LocalDate.of(2004, 10, 1))
                            .copy(foedselsnummer = SOEKER2_FOEDSELSNUMMER),
                    ),
            )

        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns
            listOf(
                DoedshendelseInternal.nyHendelse(
                    avdoed.foedselsnummer.verdi.value,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = SOEKER_FOEDSELSNUMMER.value,
                    relasjon = Relasjon.BARN,
                    Endringstype.OPPRETTET,
                ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 2) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal ikke opprette doedshendelser dersom avdoed ikke er registert som avdoed i PDL`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns
            avdoed.copy(doedsdato = null)
        every { dao.opprettDoedshendelse(any()) } just runs

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 0) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `mellom18og20PaaReformtidspunkt skal returnere riktig verdi`() {
        // Personer som er under 18 år har allerede hatt rett etter gammelt regelverk
        val personUnder18AarReformtidspunkt = personOpplysning(foedselsdato = LocalDate.of(2006, 1, 1))
        mellom18og20PaaReformtidspunkt(personUnder18AarReformtidspunkt) shouldBe false

        // Personer som blir 18 i desember kan være aktuell for informasjonsbrev
        val person18AarJanuarEtterReformtidspunkt = personOpplysning(foedselsdato = LocalDate.of(2005, 12, 31))
        mellom18og20PaaReformtidspunkt(person18AarJanuarEtterReformtidspunkt) shouldBe true

        // Personer som allerede er 20 før reformtidspunkt er ikke aktuelle for informasjonsbrev
        val person20AarFoerReformtidspunkt = personOpplysning(foedselsdato = LocalDate.of(2003, 12, 31))
        mellom18og20PaaReformtidspunkt(person20AarFoerReformtidspunkt) shouldBe false

        // Personer som blir 20 i januar etter reformtidspunkt er aktuell for informasjonsbrev (rett til og med mnd man fyller 20)
        val person20AarSluttenAvJanuarEtterReformtidspunkt = personOpplysning(foedselsdato = LocalDate.of(2004, 1, 31))
        mellom18og20PaaReformtidspunkt(person20AarSluttenAvJanuarEtterReformtidspunkt) shouldBe true
    }

    @Test
    fun `merEnnEller18PaaVirkningstidspunkt skal returnere riktig verdi`() {
        val doedsdato = LocalDate.of(2023, 1, 1)

        // Personer under 18 på virkningstidspunkt skal ikke ha informasjonsbrev
        val personUnder18AarPaaVirkningstidspunkt = personOpplysning(foedselsdato = LocalDate.of(2005, 2, 2))
        merEnnEller18PaaVirkningstidspunkt(personUnder18AarPaaVirkningstidspunkt, doedsdato) shouldBe false

        // Personer over 18 på virkningstidspunkt skal ha informasjonsbrev
        val personOver18AarPaaVirkningstidspunkt = personOpplysning(foedselsdato = LocalDate.of(2005, 2, 1))
        merEnnEller18PaaVirkningstidspunkt(personOver18AarPaaVirkningstidspunkt, doedsdato) shouldBe true
    }
}
