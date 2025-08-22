package no.nav.etterlatte.tilgangsstyring

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlientImpl
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakLesDao
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakTilgang
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.SecureRandom
import java.time.LocalDate

class OppdatererTilgangServiceTest {
    private val skjermingKlient = mockk<SkjermingKlientImpl>()
    private val pdltjenesterKlient = mockk<PdlTjenesterKlient>()
    private val sakSkrivDao = mockk<SakSkrivDao>()
    private val sakLesDao = mockk<SakLesDao>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>()
    private val brukerService = mockk<BrukerService>()
    private val sakTilgang = mockk<SakTilgang>()
    private val oppdaterTilgangService =
        OppdaterTilgangService(
            skjermingKlient,
            pdltjenesterKlient,
            brukerService,
            oppgaveService,
            sakSkrivDao,
            sakTilgang,
            sakLesDao,
        )
    private val soeker = "11057523044"
    private val persongalleri = persongalleri()

    private val defaultEnhetFraBrukerService = Enheter.PORSGRUNN

    @BeforeEach
    fun setUp() {
        every { sakTilgang.oppdaterAdressebeskyttelse(any(), any()) } just Runs
        every { sakTilgang.settEnhetOmAdressebeskyttet(any(), any()) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } just Runs
        every { sakTilgang.oppdaterSkjerming(any(), any()) } just Runs
        every { sakSkrivDao.oppdaterEnhet(any()) } just Runs
        every {
            brukerService.finnEnhetForPersonOgTema(
                soeker,
                SakType.BARNEPENSJON.tema,
                SakType.BARNEPENSJON,
            )
        } returns
            ArbeidsFordelingEnhet(
                defaultEnhetFraBrukerService.navn,
                defaultEnhetFraBrukerService.enhetNr,
            )
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(sakTilgang, oppgaveService, sakSkrivDao, sakTilgang)
    }

    @Test
    fun `Skal sette STRENGT_FORTROLIG hvis det finnes på en ident persongalleriet uavhengig av skjermet status`() {
        val sak = bpSak(enhet = Enheter.PORSGRUNN.enhetNr, gradering = null, erSkjermet = false)
        every { sakLesDao.hentSak(sak.id) } returns sak
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery {
            pdltjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(HELSOESKEN2_FOEDSELSNUMMER.value),
                    sak.sakType,
                ),
            )
        } returns AdressebeskyttelseGradering.STRENGT_FORTROLIG

        coEvery { skjermingKlient.personErSkjermet(any()) } returns true

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sak.id, persongalleri)

        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            sakTilgang.settEnhetOmAdressebeskyttet(sak, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sak.id, Enheter.STRENGT_FORTROLIG.enhetNr)))
        }
    }

    @Test
    fun `Skal sette EGEN_ANSATT hvis den finnes og ingen gradering`() {
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { pdltjenesterKlient.hentPerson(soeker, any(), any()) } returns soekerPerson(soeker)
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        coEvery { skjermingKlient.personErSkjermet(GJENLEVENDE_FOEDSELSNUMMER.value) } returns true

        val sak = bpSak(enhet = Enheter.PORSGRUNN.enhetNr, gradering = null, erSkjermet = false)
        every { sakLesDao.hentSak(sak.id) } returns sak

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sak.id, persongalleri)

        verify(exactly = 1) {
            sakTilgang.oppdaterSkjerming(sak.id, true)
            sakSkrivDao.oppdaterEnhet(SakMedEnhet(sak.id, Enheter.EGNE_ANSATTE.enhetNr))
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sak.id, Enheter.EGNE_ANSATTE.enhetNr)))
            sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT)
        }
    }

    @Test
    fun `Skal ikke oppdatere enheter om det er en vanlig sak(ikke adressebeskyttelse eller egen ansatt)`() {
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { pdltjenesterKlient.hentPerson(soeker, any(), any()) } returns soekerPerson(soeker)
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false

        val sak = bpSak(enhet = Enheter.PORSGRUNN.enhetNr, gradering = null, erSkjermet = false)
        every { sakLesDao.hentSak(sak.id) } returns sak

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sak.id, persongalleri)

        verify(exactly = 0) {
            sakTilgang.settEnhetOmAdressebeskyttet(any(), any())
            sakTilgang.oppdaterSkjerming(any(), any())
            sakSkrivDao.oppdaterEnhet(any())
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT)
        }
    }

    @Test
    fun `Skal sette ny enhet hvis saken var egen ansatt men ikke er det lenger`() {
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { pdltjenesterKlient.hentPerson(soeker, any(), any()) } returns soekerPerson(soeker)
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false

        val sak = bpSak(enhet = Enheter.EGNE_ANSATTE.enhetNr, gradering = null, erSkjermet = false)
        every { sakLesDao.hentSak(sak.id) } returns sak

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sak.id, persongalleri)

        verify(exactly = 0) {
            sakTilgang.settEnhetOmAdressebeskyttet(any(), any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT)
            sakTilgang.oppdaterSkjerming(sak.id, false)
            sakSkrivDao.oppdaterEnhet(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr))
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr)))
        }
    }

    companion object {
        @JvmStatic
        fun hentAdressebeskyttelse(): List<Pair<AdressebeskyttelseGradering, Enhetsnummer>> =
            AdressebeskyttelseGradering.entries
                .map {
                    when (it) {
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND ->
                            Pair(
                                it,
                                Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr,
                            )

                        AdressebeskyttelseGradering.STRENGT_FORTROLIG -> Pair(it, Enheter.STRENGT_FORTROLIG.enhetNr)
                        // er en virtuell enhet som vi ikke vet hva er
                        AdressebeskyttelseGradering.FORTROLIG -> Pair(it, Enhetsnummer("1234"))
                        AdressebeskyttelseGradering.UGRADERT -> Pair(it, Enheter.STEINKJER.enhetNr)
                    }
                }.filter {
                    it.first.harAdressebeskyttelse()
                }
    }

    @ParameterizedTest
    @MethodSource("hentAdressebeskyttelse")
    fun `Skal sette enheter hvis saken ikke har adressebeskyttelse lenger`(
        graderingOgEnhet: Pair<AdressebeskyttelseGradering, Enhetsnummer>,
    ) {
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { pdltjenesterKlient.hentPerson(soeker, any(), any()) } returns soekerPerson(soeker)
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false

        val sak = bpSak(enhet = graderingOgEnhet.second, gradering = graderingOgEnhet.first, erSkjermet = false)
        every { sakLesDao.hentSak(sak.id) } returns sak

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sak.id, persongalleri)

        verify(exactly = 0) {
            sakTilgang.settEnhetOmAdressebeskyttet(any(), any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT)
            sakTilgang.oppdaterSkjerming(sak.id, false)
            sakSkrivDao.oppdaterEnhet(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr))
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr)))
        }
    }

    @Test
    fun `Skal fjerne skjerming hvis soeker BP er over 18 aar og selv ikke er skjermet`() {
        val soeker18Aar = soekerPerson(soeker, foedselsdato = LocalDate.now().minusYears(18))
        coEvery {
            pdltjenesterKlient.hentPerson(soeker, any(), any())
        } returns soeker18Aar
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        coEvery { skjermingKlient.personErSkjermet(persongalleri.gjenlevende.first()) } returns true

        val sak =
            bpSak(
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
                gradering = AdressebeskyttelseGradering.UGRADERT,
                erSkjermet = true,
            )
        every { sakLesDao.hentSak(sak.id) } returns sak

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sak.id, persongalleri)

        verify(exactly = 0) {
            sakTilgang.settEnhetOmAdressebeskyttet(any(), any())
        }
        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT)
            sakTilgang.oppdaterSkjerming(sak.id, false)
            sakSkrivDao.oppdaterEnhet(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr))
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr)))
        }
    }

    @Test
    fun `Skal ikke fjerne gradering og skjerming hvis soeker er fylt 18 aar og andre i saken har adressebeskyttelse`() {
        val soeker18Aar = soekerPerson(soeker, foedselsdato = LocalDate.now().minusYears(18))
        coEvery {
            pdltjenesterKlient.hentPerson(soeker, any(), any())
        } returns soeker18Aar
        coEvery {
            pdltjenesterKlient.hentAdressebeskyttelseForPerson(any())
        } returns AdressebeskyttelseGradering.UGRADERT
        coEvery {
            pdltjenesterKlient.hentAdressebeskyttelseForPerson(match { it.ident.value == persongalleri.soesken.first() })
        } returns AdressebeskyttelseGradering.FORTROLIG

        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        coEvery { skjermingKlient.personErSkjermet(persongalleri.gjenlevende.first()) } returns true

        val sak = bpSak(enhet = Enheter.STEINKJER.enhetNr, gradering = AdressebeskyttelseGradering.FORTROLIG, erSkjermet = true)
        every { sakLesDao.hentSak(sak.id) } returns sak

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sak.id, persongalleri)

        verify(exactly = 1) {
            sakTilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.FORTROLIG)
            sakSkrivDao.oppdaterEnhet(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr))
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(listOf(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr)))
        }
        verify(exactly = 0) {
            sakTilgang.settEnhetOmAdressebeskyttet(any(), any())
            sakTilgang.oppdaterSkjerming(sak.id, false)
        }
    }

    @Test
    fun `Skal fjerne skjerming fra sak`() {
        val sak = bpSak(enhet = Enheter.EGNE_ANSATTE.enhetNr, gradering = null, erSkjermet = false)
        every { sakLesDao.hentSak(sak.id) } returns sak

        oppdaterTilgangService.fjernSkjermingFraSak(sak, soeker)

        verify(exactly = 1) {
            sakSkrivDao.oppdaterEnhet(SakMedEnhet(sak.id, defaultEnhetFraBrukerService.enhetNr))
            sakTilgang.oppdaterSkjerming(sak.id, false)
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(
                match {
                    it.first().id == sak.id &&
                        it.first().enhet == defaultEnhetFraBrukerService.enhetNr
                },
            )
        }
        verify(exactly = 0) {
            sakTilgang.oppdaterAdressebeskyttelse(any(), any())
        }
    }

    private fun bpSak(
        enhet: Enhetsnummer,
        gradering: AdressebeskyttelseGradering?,
        erSkjermet: Boolean,
    ): Sak =
        Sak(
            soeker,
            SakType.BARNEPENSJON,
            SakId(SecureRandom().nextLong(9999)),
            enhet,
            gradering,
            erSkjermet,
        )

    fun soekerPerson(
        soekerFnr: String,
        foedselsdato: LocalDate = LocalDate.now().minusYears(15),
    ): Person =
        Person(
            fornavn = "Ola",
            mellomnavn = null,
            etternavn = "Nordmann",
            foedselsnummer = Folkeregisteridentifikator.of(soekerFnr),
            foedselsdato = foedselsdato,
            foedselsaar = foedselsdato.year,
            foedeland = null,
            doedsdato = null,
            adressebeskyttelse = null,
            bostedsadresse = null,
            deltBostedsadresse = null,
            kontaktadresse = null,
            oppholdsadresse = null,
            sivilstatus = null,
            sivilstand = null,
            statsborgerskap = null,
            pdlStatsborgerskap = null,
            utland = null,
            familieRelasjon = null,
            avdoedesBarn = null,
            avdoedesBarnUtenIdent = null,
            vergemaalEllerFremtidsfullmakt = null,
        )
}
