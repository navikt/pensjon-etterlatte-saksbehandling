package no.nav.etterlatte.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.ResponseException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.EnhetService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.etterlatte.token.Saksbehandler
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
    private val enhetService = mockk<EnhetService>()
    private val skjermingKlient = mockk<SkjermingKlient>()

    @BeforeEach
    fun before() {
        clearAllMocks()
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDao, pdlKlient, norg2Klient)
    }

    private fun saksbehandlerKontekst(nasjonalTilgang: Boolean = false) {
        val tokenValidationContext = mockk<TokenValidationContext>()

        val token = mockk<JwtToken>()

        val claims = mockk<JwtTokenClaims>()

        every { claims.getAsList(any()) } returns listOf("")

        every { claims.getStringClaim(any()) } returns "Z123456"

        every { claims.containsClaim("groups", AzureGroup.NASJONAL_MED_LOGG.name) } returns nasjonalTilgang
        every { claims.containsClaim("groups", AzureGroup.NASJONAL_UTEN_LOGG.name) } returns nasjonalTilgang

        every { token.jwtTokenClaims } returns claims

        every { tokenValidationContext.getJwtToken(any()) } returns token

        val groups = AzureGroup.values().associateWith { it.name }

        Kontekst.set(
            Context(
                SaksbehandlerMedEnheterOgRoller(
                    tokenValidationContext,
                    enhetService,
                    SaksbehandlerMedRoller(
                        Saksbehandler("", "Z123456", claims),
                        groups,
                    ),
                ),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                },
            ),
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
                },
            ),
        )
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler`() {
        saksbehandlerKontekst()

        coEvery { enhetService.enheterForIdent(any()) } returns
            listOf(
                SaksbehandlerEnhet(id = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
            )

        every { sakDao.finnSaker(KONTANT_FOT.value) } returns
            listOf(
                Sak(
                    id = 1,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                ),
            )

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 1

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler uten riktig enhet`() {
        saksbehandlerKontekst()

        coEvery { enhetService.enheterForIdent(any()) } returns
            listOf(
                SaksbehandlerEnhet(id = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
            )

        every { sakDao.finnSaker(KONTANT_FOT.value) } returns
            listOf(
                Sak(
                    id = 1,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                ),
            )

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 0

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `enhet filtrering skjer ikke hvis vi har en system bruker`() {
        systemBrukerKontekst()

        coEvery { enhetService.enheterForIdent(any()) } returns
            listOf(
                SaksbehandlerEnhet(id = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
            )

        every { sakDao.finnSaker(KONTANT_FOT.value) } returns
            listOf(
                Sak(
                    id = 1,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                ),
            )

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 1

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `finnEllerOpprettSak feiler hvis PDL ikke finner geografisk tilknytning`() {
        val responseException = ResponseException(mockk(), "Oops")
        every {
            pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON)
        } throws responseException

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val thrown =
            assertThrows<ResponseException> {
                service.finnEllerOpprettSak(
                    KONTANT_FOT.value,
                    SakType.BARNEPENSJON,
                )
            }

        thrown.message shouldContain "Oops"

        verify(exactly = 0) { sakDao.finnSaker(KONTANT_FOT.value) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify {
            listOf(norg2Klient) wasNot Called
        }
    }

    @Test
    fun `finnEllerOpprettSak feiler hvis NORG2 ikke finner geografisk tilknytning`() {
        every {
            pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON)
        } returns GeografiskTilknytning(kommune = "0301", ukjent = false)
        every { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") } returns emptyList()

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val thrown =
            assertThrows<IngenEnhetFunnetException> {
                service.finnEllerOpprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON)
            }

        thrown.tema shouldBe SakType.BARNEPENSJON.tema
        thrown.omraade shouldBe "0301"

        verify(exactly = 0) { sakDao.finnSaker(KONTANT_FOT.value) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") }
    }

    @Test
    fun `finnEllerOpprettSak lagre enhet hvis enhet er funnet`() {
        every { sakDao.finnSaker(KONTANT_FOT.value) } returns emptyList()
        every {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        } returns
            Sak(
                id = 1,
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )
        coEvery { skjermingKlient.personErSkjermet(KONTANT_FOT.value) } returns false
        every { sakDao.markerSakerMedSkjerming(any(), any()) } returns 0
        every {
            pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON)
        } returns GeografiskTilknytning(kommune = "0301", ukjent = false)
        every { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.PORSGRUNN.navn, Enheter.PORSGRUNN.enhetNr),
            )

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val sak = service.finnEllerOpprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON)

        sak shouldBe
            Sak(
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        verify(exactly = 1) { sakDao.markerSakerMedSkjerming(any(), any()) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") }
        verify(exactly = 1) {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        }
    }

    @Test
    fun `finnEllerOpprettSak lagre enhet og setter gradering`() {
        systemBrukerKontekst()
        every { sakDao.finnSaker(KONTANT_FOT.value) } returns emptyList()
        coEvery { skjermingKlient.personErSkjermet(KONTANT_FOT.value) } returns false
        every { sakDao.markerSakerMedSkjerming(any(), any()) } returns 0
        every {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        } returns
            Sak(
                id = 1,
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )
        every {
            pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON)
        } returns GeografiskTilknytning(kommune = "0301", ukjent = false)
        every { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.PORSGRUNN.navn, Enheter.PORSGRUNN.enhetNr),
            )
        every { sakDao.oppdaterAdresseBeskyttelse(any(), any()) } returns 1

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val sak =
            service.finnEllerOpprettSak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                gradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
            )

        sak shouldBe
            Sak(
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        verify(exactly = 1) {
            service.oppdaterAdressebeskyttelse(
                sak.id,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
            )
        }
        verify(exactly = 1) { sakDao.markerSakerMedSkjerming(any(), any()) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") }
        verify(exactly = 1) {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        }
    }

    @Test
    fun `filtrerer for saksbehandler med nasjonal tilgang`() {
        saksbehandlerKontekst(nasjonalTilgang = true)

        coEvery { enhetService.enheterForIdent(any()) } returns
            listOf(
                SaksbehandlerEnhet(id = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
            )

        every { sakDao.finnSaker(KONTANT_FOT.value) } returns
            listOf(
                Sak(
                    id = 1,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.STEINKJER.enhetNr,
                ),
            )

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 1

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `skal sette skjerming hvis skjermingstjenesten sier at person er skjermet`() {
        saksbehandlerKontekst()
        every { sakDao.finnSaker(KONTANT_FOT.value) } returns emptyList()
        every {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr)
        } returns
            Sak(
                id = 1,
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
            )
        coEvery { skjermingKlient.personErSkjermet(KONTANT_FOT.value) } returns true
        every { sakDao.oppdaterEnheterPaaSaker(any()) } just runs
        every { sakDao.markerSakerMedSkjerming(any(), any()) } returns 1
        every {
            pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON)
        } returns GeografiskTilknytning(kommune = "0301", ukjent = false)
        every { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.EGNE_ANSATTE.navn, Enheter.EGNE_ANSATTE.enhetNr),
            )

        val service: SakService =
            SakServiceImpl(sakDao, pdlKlient, norg2Klient, skjermingKlient)
        val sak = service.finnEllerOpprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON)

        sak shouldBe
            Sak(
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
            )

        verify(exactly = 1) { sakDao.markerSakerMedSkjerming(any(), any()) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
        verify(exactly = 1) { pdlKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { norg2Klient.hentEnheterForOmraade(SakType.BARNEPENSJON.tema, "0301") }
        verify(exactly = 1) {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr)
        }
        verify(exactly = 1) { sakDao.oppdaterEnheterPaaSaker(any()) }
    }
}
