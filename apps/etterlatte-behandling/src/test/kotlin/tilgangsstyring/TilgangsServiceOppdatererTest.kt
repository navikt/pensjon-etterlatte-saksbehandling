package no.nav.etterlatte.tilgangsstyring

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
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
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.Test

class TilgangsServiceOppdatererTest {
    private val skjermingKlient = mockk<SkjermingKlient>()
    private val pdltjenesterKlient = mockk<PdlTjenesterKlient>()
    private val sakService = mockk<SakService>()
    private val oppdaterTilgangService = OppdaterTilgangService(sakService, skjermingKlient, pdltjenesterKlient)
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
        every { sakService.settEnhetOmAdresebeskyttet(sak, AdressebeskyttelseGradering.STRENGT_FORTROLIG) } just Runs
        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 1) {
            sakService.oppdaterAdressebeskyttelse(sakId, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            sakService.settEnhetOmAdresebeskyttet(sak, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
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
        oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri)

        verify(exactly = 1) {
            sakService.markerSakerMedSkjerming(match { it.first().sakId == sak.id.sakId }, true)
            sakService.oppdaterEnhetForSaker(match { it.first().id == sak.id })
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
            sakService.settEnhetOmAdresebeskyttet(any(), any())
            sakService.markerSakerMedSkjerming(any(), any())
            sakService.oppdaterEnhetForSaker(any())
        }
    }
}
