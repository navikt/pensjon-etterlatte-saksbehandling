package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.personOpplysning
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class DoedshendelseServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val dao = mockk<DoedshendelseDao>()

    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val service =
        DoedshendelseService(
            pdlTjenesterKlient = pdlTjenesterKlient,
            doedshendelseDao = dao,
            featureToggleService = toggle,
        )

    private val avdoed =
        mockPerson().copy(
            doedsdato = OpplysningDTO(LocalDate.now().minusYears(2), null),
            avdoedesBarn =
                listOf(
                    personOpplysning(foedselsdato = LocalDate.now().minusYears(23)),
                    personOpplysning(foedselsdato = LocalDate.now().minusYears(22)),
                    personOpplysning().copy(foedselsaar = LocalDate.now().minusYears(2).year),
                    personOpplysning(foedselsdato = LocalDate.now().minusYears(18)),
                    personOpplysning(foedselsdato = LocalDate.now().minusYears(18), doedsdato = LocalDate.now().minusYears(4)),
                    personOpplysning(foedselsdato = LocalDate.now().minusYears(3)),
                ),
        )

    @BeforeEach
    fun beforeAll() {
        nyKontekstMedBruker(mockk())
    }

    @Test
    fun `Skal opprette doedshendelse for samboer med felles barn`() {
        val annenForelder = JOVIAL_LAMA
        val fellesbarn =
            avdoed.avdoedesBarn!![0].copy(
                familieRelasjon = FamilieRelasjon(foreldre = listOf(annenForelder), ansvarligeForeldre = emptyList(), barn = emptyList()),
            )

        val bostedsadresse =
            listOf(
                OpplysningDTO(
                    Adresse(
                        type = AdresseType.VEGADRESSE,
                        aktiv = false,
                        coAdresseNavn = "Hos Geir",
                        adresseLinje1 = "Testveien 4",
                        adresseLinje2 = null,
                        adresseLinje3 = null,
                        postnr = "1234",
                        poststed = null,
                        land = "NOR",
                        kilde = "FREG",
                        gyldigFraOgMed = Tidspunkt.now().toLocalDatetimeUTC().minusYears(1),
                        gyldigTilOgMed = null,
                    ),
                    UUID.randomUUID().toString(),
                ),
                OpplysningDTO(
                    Adresse(
                        type = AdresseType.VEGADRESSE,
                        aktiv = true,
                        coAdresseNavn = "Hos Svein",
                        adresseLinje1 = "Gjemmeveien 4",
                        adresseLinje2 = null,
                        adresseLinje3 = null,
                        postnr = "1212",
                        poststed = null,
                        land = "NOR",
                        kilde = "FREG",
                        gyldigFraOgMed = Tidspunkt.now().toLocalDatetimeUTC().minusYears(4),
                        gyldigTilOgMed = null,
                    ),
                    UUID.randomUUID().toString(),
                ),
            )

        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                PersonRolle.AVDOED,
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns avdoed.copy(avdoedesBarn = listOf(fellesbarn), bostedsadresse = bostedsadresse)

        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                annenForelder.value,
                PersonRolle.TILKNYTTET_BARN,
                SakType.OMSTILLINGSSTOENAD,
            )
        } returns avdoed.copy(foedselsnummer = OpplysningDTO(annenForelder, "fødselsnummer"))

        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        service.opprettDoedshendelseForBeroertePersoner(
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            ),
        )
        // En for avdød og en for samboer
        verify(exactly = 2) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal opprette doedshendelse en for avdød uten barn`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns avdoed.copy(avdoedesBarn = null)
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        service.opprettDoedshendelseForBeroertePersoner(
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            ),
        )

        verify(exactly = 1) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal opprette doedshendelse med barna som kan ha rett paa barnepensjon ved doedsfall`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns avdoed
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        service.opprettDoedshendelseForBeroertePersoner(
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            ),
        )

        verify(exactly = 4) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal lagre nye hendelser hvis nye beroerte finnes`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns avdoed
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        val doedshendelseOpprettet =
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            )
        service.opprettDoedshendelseForBeroertePersoner(
            doedshendelseOpprettet,
        )
        verify(exactly = 4) {
            dao.opprettDoedshendelse(any())
        }

        every { dao.hentDoedshendelserForPerson(any()) } returns
            listOf(
                DoedshendelseInternal.nyHendelse(
                    doedshendelseOpprettet.fnr,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = SOEKER_FOEDSELSNUMMER.value,
                    relasjon = Relasjon.BARN,
                    Endringstype.OPPRETTET,
                ),
            )

        val korrigertPdlhendelse =
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.KORRIGERT,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            )

        val barnfemtenaar = LocalDate.now().minusYears(15)
        val nyttBarn = personOpplysning(foedselsdato = barnfemtenaar).copy(foedselsnummer = HELSOESKEN_FOEDSELSNUMMER)
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns avdoed.copy(avdoedesBarn = listOf(nyttBarn))
        every { dao.oppdaterDoedshendelse(any()) } just runs
        service.opprettDoedshendelseForBeroertePersoner(
            korrigertPdlhendelse,
        )
        verify(exactly = 1) {
            dao.opprettDoedshendelse(
                match {
                    it.beroertFnr == HELSOESKEN_FOEDSELSNUMMER.value
                },
            )
        }
        verify(exactly = 2) { dao.hentDoedshendelserForPerson(any()) }
        verify(exactly = 1) {
            dao.oppdaterDoedshendelse(
                match {
                    it.status == Status.OPPDATERT
                },
            )
        }
        confirmVerified(dao)
    }

    @Test
    fun `Skal oppdatere korrigert hendelse`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns avdoed
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        val doedshendelseOpprettet =
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            )
        service.opprettDoedshendelseForBeroertePersoner(
            doedshendelseOpprettet,
        )

        every { dao.hentDoedshendelserForPerson(any()) } returns
            listOf(
                DoedshendelseInternal.nyHendelse(
                    doedshendelseOpprettet.fnr,
                    avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                    beroertFnr = SOEKER_FOEDSELSNUMMER.value,
                    relasjon = Relasjon.BARN,
                    Endringstype.OPPRETTET,
                ),
            )

        val korrigertPdlhendelse =
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.KORRIGERT,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            )

        every { dao.oppdaterDoedshendelse(any()) } just runs
        service.opprettDoedshendelseForBeroertePersoner(
            korrigertPdlhendelse,
        )

        verify(exactly = 4) {
            dao.opprettDoedshendelse(any())
        }
        verify(exactly = 2) { dao.hentDoedshendelserForPerson(any()) }
        verify(exactly = 1) {
            dao.oppdaterDoedshendelse(
                match {
                    it.status == Status.OPPDATERT &&
                        it.endringstype == Endringstype.KORRIGERT &&
                        it.avdoedDoedsdato == avdoed.doedsdato?.verdi
                },
            )
        }
        confirmVerified(dao)
    }

    @Test
    fun `Skal oppdatere annullert hendelse`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns avdoed
        every { dao.opprettDoedshendelse(any()) } just runs
        every { dao.hentDoedshendelserForPerson(any()) } returns emptyList()

        val doedshendelseOpprettet =
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            )
        service.opprettDoedshendelseForBeroertePersoner(
            doedshendelseOpprettet,
        )

        val eksisterendeHendelse =
            DoedshendelseInternal.nyHendelse(
                doedshendelseOpprettet.fnr,
                avdoedDoedsdato = avdoed.doedsdato!!.verdi,
                beroertFnr = SOEKER_FOEDSELSNUMMER.value,
                relasjon = Relasjon.BARN,
                Endringstype.OPPRETTET,
            )
        every { dao.hentDoedshendelserForPerson(any()) } returns
            listOf(
                eksisterendeHendelse,
            )

        val annullertPdlHendelse =
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.ANNULLERT,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            )

        every { dao.oppdaterDoedshendelse(any()) } just runs
        service.opprettDoedshendelseForBeroertePersoner(
            annullertPdlHendelse,
        )

        verify(exactly = 4) {
            dao.opprettDoedshendelse(any())
        }
        verify(exactly = 2) { dao.hentDoedshendelserForPerson(any()) }
        verify(exactly = 1) { dao.oppdaterDoedshendelse(any()) }
        verify(exactly = 1) {
            dao.oppdaterDoedshendelse(
                match {
                    it.status == Status.FERDIG && it.utfall == Utfall.AVBRUTT && it.endringstype == Endringstype.ANNULLERT
                },
            )
        }
        confirmVerified(dao)
    }

    @Test
    fun `Skal ikke opprette doedshendelser dersom avdoed ikke er registert som avdoed i PDL`() {
        every {
            pdlTjenesterKlient.hentPdlModellFlereSaktyper(
                avdoed.foedselsnummer.verdi.value,
                any(),
                listOf(SakType.BARNEPENSJON, SakType.OMSTILLINGSSTOENAD),
            )
        } returns
            avdoed.copy(doedsdato = null)
        every { dao.opprettDoedshendelse(any()) } just runs

        service.opprettDoedshendelseForBeroertePersoner(
            DoedshendelsePdl(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            ),
        )

        verify(exactly = 0) {
            dao.opprettDoedshendelse(any())
        }
    }
}
