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
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test

class TilgangsServiceOppdatererTest {
    private val skjermingKlient = mockk<SkjermingKlientImpl>()
    private val pdltjenesterKlient = mockk<PdlTjenesterKlient>()
    private val sakService = mockk<SakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val brukerService = mockk<BrukerService>()

    private val oppdaterTilgangService =
        OppdaterTilgangService(sakService, skjermingKlient, pdltjenesterKlient, brukerService, oppgaveService)
    private val soeker = "11057523044"
    private val persongalleri =
        Persongalleri(
            soeker,
            INNSENDER_FOEDSELSNUMMER.value,
            listOf(HELSOESKEN2_FOEDSELSNUMMER.value, HALVSOESKEN_FOEDSELSNUMMER.value),
            listOf(AVDOED_FOEDSELSNUMMER.value),
            listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
        )

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
        every { sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG) } returns 1
        every { sakService.settEnhetOmAdresseebeskyttet(sak, AdressebeskyttelseGradering.STRENGT_FORTROLIG) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } just Runs

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 1) {
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            sakService.settEnhetOmAdresseebeskyttet(sak, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
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

        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 1) {
            sakService.markerSakerMedSkjerming(match { it.first().value == sak.id.value }, true)
            sakService.oppdaterEnhetForSaker(match { it.first().id == sak.id })
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(match { it.first().id == sak.id })
        }
    }

    @Test
    fun `Skal ikke sette noe om gradering og egen ansatt ikke finnes`() {
        val saktype = SakType.BARNEPENSJON
        coEvery { pdltjenesterKlient.hentAdressebeskyttelseForPerson(any()) } returns AdressebeskyttelseGradering.UGRADERT
        coEvery { skjermingKlient.personErSkjermet(any()) } returns false

        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, Enheter.PORSGRUNN.enhetNr)

        every { sakService.finnSak(sakId) } returns sak
        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 0) {
            sakService.oppdaterAdressebeskyttelse(any(), any())
            sakService.settEnhetOmAdresseebeskyttet(any(), any())
            sakService.markerSakerMedSkjerming(any(), any())
            sakService.oppdaterEnhetForSaker(any())
        }
    }

    @Test
    fun `Skal fjerne skjerming fra sak`() {
        val saktype = SakType.BARNEPENSJON
        val sakId = SakId(1L)
        val sak = Sak(soeker, saktype, sakId, Enheter.EGNE_ANSATTE.enhetNr)

        every { brukerService.finnEnhetForPersonOgTema(soeker, SakType.BARNEPENSJON.tema, SakType.BARNEPENSJON) } returns
            ArbeidsFordelingEnhet(
                Enheter.defaultEnhet.navn,
                Enheter.defaultEnhet.enhetNr,
            )
        every { sakService.oppdaterEnhetForSaker(any()) } just Runs
        every { sakService.markerSakerMedSkjerming(any(), false) } just Runs
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } just Runs

        oppdaterTilgangService.fjernSkjermingFraSak(sak, soeker)

        verify(exactly = 1) {
            sakService.oppdaterEnhetForSaker(match { it.first().id == sak.id && it.first().enhet == Enheter.defaultEnhet.enhetNr })
            sakService.markerSakerMedSkjerming(match { it.first().sakId == sak.id.sakId }, false)
            oppgaveService.oppdaterEnhetForRelaterteOppgaver(
                match {
                    it.first().id == sak.id &&
                        it.first().enhet == Enheter.defaultEnhet.enhetNr
                },
            )
        }
    }
}
