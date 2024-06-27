package grunnlagsendring.doedshendelse.mellomAttenOgTjueVedReformtidspunkt

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
                    personOpplysning(foedselsdato = LocalDate.of(2006, 1, 1)),
                    personOpplysning(foedselsdato = LocalDate.of(2006, 1, 2)),
                ),
        )

    @BeforeEach
    fun beforeAll() {
        nyKontekstMedBruker(mockk())
    }

    @Test
    fun `Skal ikke opprette doedshendelse en for avdød som ikke har barn`() {
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
    fun `Skal opprette doedshendelse for avdød og barna som er mellom 18 og 20 år på reformtidspunkt`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON),
            )
        } returns avdoed
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 4) {
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
                        personOpplysning(foedselsdato = LocalDate.of(2005, 6, 1)),
                    ),
            )
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 2) {
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
                        personOpplysning(foedselsdato = LocalDate.of(2005, 6, 1)),
                        personOpplysning(foedselsdato = LocalDate.of(2005, 10, 1))
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
                DoedshendelseInternal.nyHendelse(
                    avdoed.foedselsnummer.verdi.value,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = avdoed.foedselsnummer.verdi.value,
                    relasjon = Relasjon.AVDOED,
                    Endringstype.OPPRETTET,
                ),
            )

        service.opprettDoedshendelse(avdoed.foedselsnummer.verdi.value)

        verify(exactly = 3) {
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
}
