package no.nav.etterlatte.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.ResponseException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SakServiceTest {
    private val sakDao = mockk<SakDao>()
    private val pdlKlient = mockk<PdlKlient>()
    private val norg2Klient = mockk<Norg2Klient>()
    private val featureToggleService = mockk<FeatureToggleService>()

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDao, pdlKlient, norg2Klient, featureToggleService)
    }

    @Test
    fun `finnEllerOpprettSak feiler hvis PDL ikke finner geografisk tilknytning`() {
        val responseException = ResponseException(mockk(), "Oops")

        every { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) } returns emptyList()
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) } returns true
        every { pdlKlient.hentGeografiskTilknytning(TRIVIELL_MIDTPUNKT.value) } throws responseException

        val service: SakService = RealSakService(sakDao, pdlKlient, norg2Klient, featureToggleService)

        val thrown = assertThrows<ResponseException> {
            service.finnEllerOpprettSak(
                TRIVIELL_MIDTPUNKT.value,
                SakType.BARNEPENSJON
            )
        }

        thrown.message shouldContain "Oops"

        verify(exactly = 1) { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(TRIVIELL_MIDTPUNKT.value) }
        verify {
            listOf(norg2Klient) wasNot Called
        }
    }

    @Test()
    fun `finnEllerOpprettSak feiler hvis NORG2 ikke finner geografisk tilknytning`() {
        every { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) } returns emptyList()
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) } returns true
        every {
            pdlKlient.hentGeografiskTilknytning(TRIVIELL_MIDTPUNKT.value)
        } returns GeografiskTilknytning(kommune = "0301", ukjent = false)
        every { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") } returns emptyList()

        val service: SakService = RealSakService(sakDao, pdlKlient, norg2Klient, featureToggleService)

        val thrown = assertThrows<IngenEnhetFunnetException> {
            service.finnEllerOpprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON)
        }

        thrown.tema shouldBe SakType.BARNEPENSJON.tema
        thrown.omraade shouldBe "0301"

        verify(exactly = 1) { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") }
    }

    @Test()
    fun `finnEllerOpprettSak lagre enhet hvis enhet er funnet`() {
        every { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) } returns emptyList()
        every {
            sakDao.opprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON, Enheter.DEFAULT.enhetNr)
        } returns Sak(
            id = 1,
            ident = TRIVIELL_MIDTPUNKT.value,
            sakType = SakType.BARNEPENSJON,
            enhet = Enheter.DEFAULT.enhetNr
        )
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) } returns true
        every {
            pdlKlient.hentGeografiskTilknytning(TRIVIELL_MIDTPUNKT.value)
        } returns GeografiskTilknytning(kommune = "0301", ukjent = false)
        every { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") } returns listOf(
            ArbeidsFordelingEnhet(Enheter.DEFAULT.navn, Enheter.DEFAULT.enhetNr)
        )

        val service: SakService = RealSakService(sakDao, pdlKlient, norg2Klient, featureToggleService)

        val sak = service.finnEllerOpprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON)

        sak shouldBe Sak(
            ident = TRIVIELL_MIDTPUNKT.value,
            sakType = SakType.BARNEPENSJON,
            id = 1,
            enhet = Enheter.DEFAULT.enhetNr
        )

        verify(exactly = 1) { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") }
        verify(exactly = 1) {
            sakDao.opprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON, Enheter.DEFAULT.enhetNr)
        }
    }

    @Test()
    fun `finnEllerOpprettSak lagre uten enhet - feature disabled`() {
        every { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) } returns emptyList()
        every {
            sakDao.opprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON, null)
        } returns Sak(
            id = 1,
            ident = TRIVIELL_MIDTPUNKT.value,
            sakType = SakType.BARNEPENSJON,
            enhet = null
        )
        every { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) } returns false

        val service: SakService = RealSakService(sakDao, pdlKlient, norg2Klient, featureToggleService)

        val sak = service.finnEllerOpprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON)

        sak shouldBe Sak(
            ident = TRIVIELL_MIDTPUNKT.value,
            sakType = SakType.BARNEPENSJON,
            id = 1,
            enhet = null
        )

        verify(exactly = 1) { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false) }
        verify(exactly = 1) { sakDao.opprettSak(TRIVIELL_MIDTPUNKT.value, SakType.BARNEPENSJON, null) }
        verify {
            listOf(pdlKlient, norg2Klient) wasNot Called
        }
    }
}