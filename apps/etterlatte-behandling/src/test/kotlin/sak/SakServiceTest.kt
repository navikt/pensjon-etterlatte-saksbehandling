package no.nav.etterlatte.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.ResponseException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.TRIVIELL_MIDTPUNKT
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection

internal class SakServiceTest {
    private val sakDao = mockk<SakDao>()
    private val pdlKlient = mockk<PdlKlient>()
    private val norg2Klient = mockk<Norg2Klient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val enhetService = mockk<EnhetService>()

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDao, pdlKlient, norg2Klient, featureToggleService)
    }

    fun saksbehandlerKontekst() {
        val tokenValidationContext = mockk<TokenValidationContext>()

        val token = mockk<JwtToken>()

        val claims = mockk<JwtTokenClaims>()

        every { claims.getAsList(any()) } returns listOf("")

        every { claims.getStringClaim(any()) } returns "Z123456"

        every { token.jwtTokenClaims } returns claims

        every { tokenValidationContext.getJwtToken(any()) } returns token

        Kontekst.set(
            Context(
                Saksbehandler(tokenValidationContext, mockk(), enhetService),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                }
            )
        )
    }

    fun systemBrukerKontekst() {
        val tokenValidationContext = mockk<TokenValidationContext>()

        val token = mockk<JwtToken>()

        val claims = mockk<JwtTokenClaims>()

        every { claims.getAsList(any()) } returns listOf("")

        every { claims.getStringClaim(any()) } returns "oid"

        every { token.subject } returns "oid"

        every { token.jwtTokenClaims } returns claims

        every { tokenValidationContext.getJwtToken(any()) } returns token

        Kontekst.set(
            Context(
                SystemUser(tokenValidationContext),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                }
            )
        )
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler`() {
        saksbehandlerKontekst()

        coEvery { enhetService.enheterForIdent(any()) } returns listOf(
            SaksbehandlerEnhet(id = Enheter.DEFAULT.enhetNr, navn = Enheter.DEFAULT.navn)
        )

        every { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) } returns listOf(
            Sak(
                id = 1,
                ident = TRIVIELL_MIDTPUNKT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.DEFAULT.enhetNr
            )
        )

        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val service: SakService = RealSakService(sakDao, pdlKlient, norg2Klient, featureToggleService)

        val saker = service.finnSaker(TRIVIELL_MIDTPUNKT.value)

        saker.size shouldBe 1

        verify(exactly = 1) { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) }
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler uten riktig enhet`() {
        saksbehandlerKontekst()

        coEvery { enhetService.enheterForIdent(any()) } returns listOf(
            SaksbehandlerEnhet(id = Enheter.DEFAULT.enhetNr, navn = Enheter.DEFAULT.navn)
        )

        every { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) } returns listOf(
            Sak(
                id = 1,
                ident = TRIVIELL_MIDTPUNKT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.STRENGT_FORTROLIG.enhetNr
            )
        )

        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val service: SakService = RealSakService(sakDao, pdlKlient, norg2Klient, featureToggleService)

        val saker = service.finnSaker(TRIVIELL_MIDTPUNKT.value)

        saker.size shouldBe 0

        verify(exactly = 1) { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) }
    }

    @Test
    fun `enhet filtrering skjer ikke hvis vi har en system bruker`() {
        systemBrukerKontekst()

        coEvery { enhetService.enheterForIdent(any()) } returns listOf(
            SaksbehandlerEnhet(id = Enheter.DEFAULT.enhetNr, navn = Enheter.DEFAULT.navn)
        )

        every { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) } returns listOf(
            Sak(
                id = 1,
                ident = TRIVIELL_MIDTPUNKT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.STRENGT_FORTROLIG.enhetNr
            )
        )

        every { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) } returns true

        val service: SakService = RealSakService(sakDao, pdlKlient, norg2Klient, featureToggleService)

        val saker = service.finnSaker(TRIVIELL_MIDTPUNKT.value)

        saker.size shouldBe 1

        verify(exactly = 1) { sakDao.finnSaker(TRIVIELL_MIDTPUNKT.value) }
        verify(exactly = 1) { featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false) }
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