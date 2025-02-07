package no.nav.etterlatte.tilgangsstyring

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlientImpl
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class OppdatererTilgangServiceTest {
    private val skjermingKlient = mockk<SkjermingKlientImpl>()
    private val pdltjenesterKlient = mockk<PdlTjenesterKlient>()
    private val sakService = mockk<SakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val brukerService = mockk<BrukerService>()

    private val oppdaterTilgangService =
        OppdaterTilgangService(sakService, skjermingKlient, pdltjenesterKlient, brukerService, oppgaveService)
    private val soeker = "11057523044"
    private val persongalleri = persongalleri()

    @Test
    fun `Skal sette STRENGT_FORTROLIG hvis det finnes på en ident persongalleriet uavhengig av skjermet status`() {
        val saktype = SakType.BARNEPENSJON
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery {
            pdltjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(HELSOESKEN2_FOEDSELSNUMMER.value),
                    saktype,
                ),
            )
        } returns AdressebeskyttelseGradering.STRENGT_FORTROLIG

        coEvery { skjermingKlient.personErSkjermet(any()) } returns true

        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, Enheter.PORSGRUNN.enhetNr)
        every { sakService.finnSak(sakId) } returns sak
        every { sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG) } just Runs
        every { sakService.settEnhetOmAdressebeskyttet(sak, AdressebeskyttelseGradering.STRENGT_FORTROLIG) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } just Runs

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 1) {
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            sakService.settEnhetOmAdressebeskyttet(sak, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id })
        }
    }

    @Test
    fun `Skal sette EGEN_ANSATT hvis den finnes og ingen gradering`() {
        val saktype = SakType.BARNEPENSJON
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        coEvery { skjermingKlient.personErSkjermet(GJENLEVENDE_FOEDSELSNUMMER.value) } returns true

        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, Enheter.PORSGRUNN.enhetNr)
        every { sakService.finnSak(sakId) } returns sak
        every { sakService.markerSakerMedSkjerming(any(), any()) } just Runs
        every { sakService.oppdaterEnhetForSaker(any()) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } just Runs
        every { sakService.oppdaterAdressebeskyttelse(any(), any()) } just Runs

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 1) {
            sakService.markerSakerMedSkjerming(match { it.first().sakId == sak.id.sakId }, true)
            sakService.oppdaterEnhetForSaker(match { it.first().id == sak.id })
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id })
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT)
        }
    }

    @Test
    fun `Skal ikke oppdatere enheter om det er en vanlig sak(ikke adressebeskyttelse eller egen ansatt)`() {
        val saktype = SakType.BARNEPENSJON
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        val enhet = Enheter.defaultEnhet
        every { brukerService.finnEnhetForPersonOgTema(soeker, SakType.BARNEPENSJON.tema, SakType.BARNEPENSJON) } returns
            ArbeidsFordelingEnhet(
                enhet.navn,
                enhet.enhetNr,
            )

        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, Enheter.PORSGRUNN.enhetNr)

        every { sakService.finnSak(sakId) } returns sak
        every { sakService.hentGraderingForSak(sakId, any(Systembruker::class)) } returns
            SakMedGraderingOgSkjermet(
                sakId,
                AdressebeskyttelseGradering.UGRADERT,
                false,
                Enheter.PORSGRUNN.enhetNr,
            )
        every { sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT) } just Runs
        every { sakService.oppdaterEnhetForSaker(any()) } just Runs
        every { sakService.markerSakerMedSkjerming(match { it.first() == sak.id }, false) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id }) } just Runs

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 0) {
            sakService.settEnhetOmAdressebeskyttet(any(), any())
            sakService.markerSakerMedSkjerming(any(), any())
            sakService.oppdaterEnhetForSaker(any())
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
        }
        verify(exactly = 1) {
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT)
        }
    }

    @Test
    fun `Skal sette ny enhet hvis saken var egen ansatt men ikke er det lenger`() {
        val saktype = SakType.BARNEPENSJON
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        val enhet = Enheter.defaultEnhet
        every { brukerService.finnEnhetForPersonOgTema(soeker, SakType.BARNEPENSJON.tema, SakType.BARNEPENSJON) } returns
            ArbeidsFordelingEnhet(
                enhet.navn,
                enhet.enhetNr,
            )

        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, Enheter.EGNE_ANSATTE.enhetNr)

        every { sakService.finnSak(sakId) } returns sak
        every { sakService.hentGraderingForSak(sakId, any(Systembruker::class)) } returns
            SakMedGraderingOgSkjermet(
                sakId,
                AdressebeskyttelseGradering.UGRADERT,
                true,
                Enheter.EGNE_ANSATTE.enhetNr,
            )
        every { sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT) } just Runs
        every { sakService.oppdaterEnhetForSaker(any()) } just Runs
        every { sakService.markerSakerMedSkjerming(match { it.first() == sak.id }, false) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id }) } just Runs

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 0) {
            sakService.settEnhetOmAdressebeskyttet(any(), any())
        }
        verify(exactly = 1) {
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT)
            sakService.markerSakerMedSkjerming(match { it.first() == sak.id }, false)
            sakService.oppdaterEnhetForSaker(match { it.first().id == sak.id && it.first().enhet == enhet.enhetNr })
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id })
        }
    }

    companion object {
        @JvmStatic
        fun hentAdressebeskyttelse(): List<Pair<AdressebeskyttelseGradering, Enhetsnummer>> =
            AdressebeskyttelseGradering.entries
                .map {
                    when (it) {
                        AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Pair(it, Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr)
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
        val saktype = SakType.BARNEPENSJON
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false
        val enhet = Enheter.defaultEnhet
        every { brukerService.finnEnhetForPersonOgTema(soeker, SakType.BARNEPENSJON.tema, SakType.BARNEPENSJON) } returns
            ArbeidsFordelingEnhet(
                enhet.navn,
                enhet.enhetNr,
            )

        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, graderingOgEnhet.second)

        every { sakService.finnSak(sakId) } returns sak
        every { sakService.hentGraderingForSak(sakId, any(Systembruker::class)) } returns
            SakMedGraderingOgSkjermet(
                sakId,
                graderingOgEnhet.first,
                false,
                graderingOgEnhet.second,
            )
        every { sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT) } just Runs
        every { sakService.oppdaterEnhetForSaker(any()) } just Runs
        every { sakService.markerSakerMedSkjerming(match { it.first() == sak.id }, false) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id }) } just Runs

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 0) {
            sakService.settEnhetOmAdressebeskyttet(any(), any())
        }
        verify(exactly = 1) {
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.UGRADERT)
            sakService.markerSakerMedSkjerming(match { it.first() == sak.id }, false)
            sakService.oppdaterEnhetForSaker(match { it.first().id == sak.id && it.first().enhet == enhet.enhetNr })
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id })
        }
    }

    @Test
    fun `Skal fjerne skjerming fra sak`() {
        val saktype = SakType.BARNEPENSJON
        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, Enheter.EGNE_ANSATTE.enhetNr)

        val enhet = Enheter.defaultEnhet
        every { brukerService.finnEnhetForPersonOgTema(soeker, SakType.BARNEPENSJON.tema, SakType.BARNEPENSJON) } returns
            ArbeidsFordelingEnhet(
                enhet.navn,
                enhet.enhetNr,
            )
        every { sakService.oppdaterEnhetForSaker(any()) } just Runs
        every { sakService.markerSakerMedSkjerming(any(), false) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } just Runs

        oppdaterTilgangService.fjernSkjermingFraSak(sak, soeker)

        verify(exactly = 1) {
            sakService.oppdaterEnhetForSaker(match { it.first().id == sak.id && it.first().enhet == enhet.enhetNr })
            sakService.markerSakerMedSkjerming(match { it.first().sakId == sak.id.sakId }, false)
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(
                match {
                    it.first().id == sak.id &&
                        it.first().enhet == enhet.enhetNr
                },
            )
        }
    }
}
