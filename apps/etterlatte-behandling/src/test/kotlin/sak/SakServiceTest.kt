package no.nav.etterlatte.sak

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.ResponseException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.PdltjenesterKlientTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.BrukerServiceImpl
import no.nav.etterlatte.behandling.GrunnlagService
import no.nav.etterlatte.behandling.IngenEnhetFunnetException
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

internal class SakServiceTest {
    private lateinit var service: SakService
    val pdlTjenesterKlient = spyk<PdltjenesterKlientTest>()
    val norg2Klient = mockk<Norg2Klient>()
    val brukerService = BrukerServiceImpl(pdlTjenesterKlient, norg2Klient)
    val saksbehandlerService = mockk<SaksbehandlerService>()
    val skjermingKlient = mockk<SkjermingKlient>()
    val sakDao = mockk<SakDao>()
    val grunnlagservice = mockk<GrunnlagService>()

    @BeforeEach
    fun before() {
        clearAllMocks()

        coEvery { grunnlagservice.leggInnNyttGrunnlagSak(any(), any()) } just runs
        coEvery { grunnlagservice.leggTilNyeOpplysningerBareSak(any(), any()) } just runs

        val krrKlient =
            mockk<KrrKlient> {
                coEvery { hentDigitalKontaktinformasjon(any()) } returns
                    DigitalKontaktinformasjon(
                        personident = "",
                        aktiv = true,
                        kanVarsles = true,
                        reservert = false,
                        spraak = "nb",
                        epostadresse = null,
                        mobiltelefonnummer = null,
                        sikkerDigitalPostkasse = null,
                    )
            }
        service = SakServiceImpl(sakDao, skjermingKlient, brukerService, grunnlagservice, krrKlient, pdlTjenesterKlient)

        every {
            sakDao.finnSakMedGraderingOgSkjerming(
                any(),
            )
        } returns SakMedGraderingOgSkjermet(1L, null, false, Enheter.defaultEnhet.enhetNr)
    }

    @AfterEach
    fun after() {
        confirmVerified(sakDao, pdlTjenesterKlient, norg2Klient)
    }

    private fun saksbehandlerKontekst(
        nasjonalTilgang: Boolean = false,
        strentFortrolig: Boolean = false,
        egenAnsatt: Boolean = false,
    ) {
        val tokenValidationContext = mockk<TokenValidationContext>()

        val token = mockk<JwtToken>()

        val tilgangsgrupper = mutableSetOf<AzureGroup>()
        if (nasjonalTilgang) {
            tilgangsgrupper.add(AzureGroup.NASJONAL_MED_LOGG)
            tilgangsgrupper.add(AzureGroup.NASJONAL_UTEN_LOGG)
        }
        if (strentFortrolig) {
            tilgangsgrupper.add(AzureGroup.STRENGT_FORTROLIG)
        }
        if (egenAnsatt) {
            tilgangsgrupper.add(AzureGroup.EGEN_ANSATT)
        }

        every { tokenValidationContext.getJwtToken(any()) } returns token

        val groups = AzureGroup.entries.associateWith { it.name }

        val saksbehandler = simpleSaksbehandler(claims = mapOf(Claims.groups to tilgangsgrupper.map { it.name }))
        nyKontekstMedBruker(
            SaksbehandlerMedEnheterOgRoller(
                tokenValidationContext,
                saksbehandlerService,
                SaksbehandlerMedRoller(
                    saksbehandler,
                    groups,
                ),
                saksbehandler,
            ),
        )
    }

    private fun systemBrukerKontekst() {
        val tokenValidationContext = mockk<TokenValidationContext>()

        val token = mockk<JwtToken>()

        val claims = mockk<JwtTokenClaims>()

        every { claims.getAsList(any()) } returns listOf("")

        every { claims.getStringClaim(any()) } returns "oid"

        every { token.subject } returns "oid"

        every { token.jwtTokenClaims } returns claims

        every { tokenValidationContext.getJwtToken(any()) } returns token

        val brukerTokenInfo = mockk<BrukerTokenInfo>()

        nyKontekstMedBruker(
            SystemUser(tokenValidationContext, brukerTokenInfo),
        )
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler`() {
        saksbehandlerKontekst()

        coEvery { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(enhetsNummer = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
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

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 1

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler uten riktig enhet`() {
        saksbehandlerKontekst()

        coEvery { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(enhetsNummer = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
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

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 0

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `enhet filtrering skjer ikke hvis vi har en system bruker`() {
        systemBrukerKontekst()

        coEvery { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(enhetsNummer = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
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

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 1

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `finn OMS sak for ident sak sin ident`() {
        saksbehandlerKontekst()
        val sakId: no.nav.etterlatte.libs.common.sak.SakId = 1
        coEvery { grunnlagservice.hentAlleSakerForPerson(KONTANT_FOT.value) } returns PersonMedSakerOgRoller(KONTANT_FOT.value, emptyList())
        every { sakDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                Sak(
                    id = sakId,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                ),
            )

        val finnSakerOmsOgHvisAvdoed = service.finnSakerOmsOgHvisAvdoed(KONTANT_FOT.value)

        finnSakerOmsOgHvisAvdoed shouldContainExactly listOf(sakId)

        coVerify { grunnlagservice.hentAlleSakerForPerson(KONTANT_FOT.value) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD) }
    }

    @Test
    fun `finn OMS sak for avdød i persongalleri på sak i finnSakerOmsOgHvisAvdoed`() {
        saksbehandlerKontekst()
        val sakId: no.nav.etterlatte.libs.common.sak.SakId = 1
        coEvery { grunnlagservice.hentAlleSakerForPerson(KONTANT_FOT.value) } returns
            PersonMedSakerOgRoller(
                KONTANT_FOT.value,
                listOf(
                    SakidOgRolle(sakId, Saksrolle.AVDOED),
                ),
            )
        every { sakDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD) } returns emptyList()

        val finnSakerOmsOgHvisAvdoed = service.finnSakerOmsOgHvisAvdoed(KONTANT_FOT.value)

        finnSakerOmsOgHvisAvdoed shouldContainExactly listOf(sakId)

        coVerify { grunnlagservice.hentAlleSakerForPerson(KONTANT_FOT.value) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD) }
    }

    @Test
    fun `finnEllerOpprettSak feiler hvis PDL ikke finner geografisk tilknytning`() {
        val responseException = ResponseException(mockk(), "Oops")
        every { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
        every {
            pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON)
        } throws responseException

        val thrown =
            assertThrows<ResponseException> {
                service.finnEllerOpprettSakMedGrunnlag(
                    KONTANT_FOT.value,
                    SakType.BARNEPENSJON,
                )
            }

        thrown.message shouldContain "Oops"

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify {
            listOf(norg2Klient) wasNot Called
        }
    }

    @Test
    fun `finnEllerOpprettSak feiler hvis NORG2 ikke finner geografisk tilknytning`() {
        every {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301"),
            )
        } returns emptyList()
        every { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()

        val thrown =
            assertThrows<IngenEnhetFunnetException> {
                service.finnEllerOpprettSakMedGrunnlag(KONTANT_FOT.value, SakType.BARNEPENSJON)
            }

        thrown.arbeidsFordelingRequest.tema shouldBe SakType.BARNEPENSJON.tema
        thrown.arbeidsFordelingRequest.geografiskOmraade shouldBe "0301"

        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301")) }
    }

    @Test
    fun `finnEllerOpprettSak lagre enhet hvis enhet er funnet`() {
        every { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
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
        every { sakDao.oppdaterAdresseBeskyttelse(1, AdressebeskyttelseGradering.UGRADERT) } returns 1

        every { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301")) } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.PORSGRUNN.navn, Enheter.PORSGRUNN.enhetNr),
            )

        val sak = service.finnEllerOpprettSakMedGrunnlag(KONTANT_FOT.value, SakType.BARNEPENSJON)

        sak shouldBe
            Sak(
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.PORSGRUNN.enhetNr,
            )

        verify(exactly = 1) { sakDao.finnSakMedGraderingOgSkjerming(any()) }
        verify(exactly = 1) { sakDao.markerSakerMedSkjerming(any(), any()) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        coVerify(exactly = 1) {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(KONTANT_FOT.value),
                    SakType.BARNEPENSJON,
                ),
            )
        }
        verify(exactly = 1) { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301")) }
        verify(exactly = 1) {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        }
        verify(exactly = 1) { sakDao.oppdaterAdresseBeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT) }
    }

    // TODO: skriv om til at pdltjenester returnerer  strengt fortrolgi for denne
    @Test
    fun `finnEllerOpprettSak lagre enhet og setter gradering`() {
        systemBrukerKontekst()
        every { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
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

        coEvery {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(PersonIdent(KONTANT_FOT.value), SakType.BARNEPENSJON),
            )
        } returns AdressebeskyttelseGradering.STRENGT_FORTROLIG

        every { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301")) } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.PORSGRUNN.navn, Enheter.PORSGRUNN.enhetNr),
            )
        every { sakDao.oppdaterAdresseBeskyttelse(any(), any()) } returns 1
        every { sakDao.oppdaterEnheterPaaSaker(any()) } just runs

        val sak =
            service.finnEllerOpprettSakMedGrunnlag(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
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
        verify(exactly = 1) { sakDao.finnSakMedGraderingOgSkjerming(any()) }
        verify(exactly = 1) { sakDao.markerSakerMedSkjerming(any(), any()) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        coVerify(exactly = 1) {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(KONTANT_FOT.value),
                    SakType.BARNEPENSJON,
                ),
            )
        }
        verify(exactly = 1) { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301")) }
        verify(exactly = 1) {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        }
        verify(exactly = 1) { sakDao.oppdaterEnheterPaaSaker(any()) }
    }

    @Test
    fun `skal sette skjerming hvis skjermingstjenesten sier at person er skjermet`() {
        saksbehandlerKontekst()
        every { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
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
        every { sakDao.oppdaterAdresseBeskyttelse(1, AdressebeskyttelseGradering.UGRADERT) } returns 1
        every { sakDao.markerSakerMedSkjerming(any(), any()) } returns 1

        every { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301")) } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.EGNE_ANSATTE.navn, Enheter.EGNE_ANSATTE.enhetNr),
            )

        val sak = service.finnEllerOpprettSakMedGrunnlag(KONTANT_FOT.value, SakType.BARNEPENSJON)

        sak shouldBe
            Sak(
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                id = 1,
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
            )

        verify(exactly = 1) { sakDao.markerSakerMedSkjerming(any(), any()) }
        verify(exactly = 1) { sakDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { sakDao.finnSakMedGraderingOgSkjerming(any()) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        coVerify(exactly = 1) {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(KONTANT_FOT.value),
                    SakType.BARNEPENSJON,
                ),
            )
        }
        verify(exactly = 1) { norg2Klient.hentArbeidsfordelingForOmraadeOgTema(ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301")) }
        verify(exactly = 1) {
            sakDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr)
        }
        verify(exactly = 1) { sakDao.oppdaterEnheterPaaSaker(any()) }
        verify(exactly = 1) { sakDao.oppdaterAdresseBeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har ingen saker`() {
        saksbehandlerKontekst()

        every { sakDao.finnSaker(any()) } returns emptyList()

        assertThrows<PersonManglerSak> {
            service.hentEnkeltSakForPerson("ident")
        }

        verify { sakDao.finnSaker(any()) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har sak, men saksbehandler mangler tilgang til enhet egne ansatte`() {
        saksbehandlerKontekst()

        val ident = Random.nextLong().toString()

        every { sakDao.finnSaker(any()) } returns
            listOf(
                Sak(
                    ident = ident,
                    sakType = SakType.BARNEPENSJON,
                    id = Random.nextLong(),
                    enhet = Enheter.EGNE_ANSATTE.enhetNr,
                ),
            )

        assertThrows<ManglerTilgangTilEnhet> {
            service.hentEnkeltSakForPerson(ident)
        }

        verify { sakDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak egen ansatt - Bruker har sak egen ansatt`() {
        saksbehandlerKontekst(egenAnsatt = true)

        val ident = Random.nextLong().toString()

        val sak =
            Sak(
                ident = ident,
                sakType = SakType.BARNEPENSJON,
                id = Random.nextLong(),
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
            )
        every { sakDao.finnSaker(any()) } returns
            listOf(
                sak,
            )

        val enkeltsak = service.hentEnkeltSakForPerson(ident)

        enkeltsak shouldBe sak

        verify { sakDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har sak, men saksbehandler mangler tilgang til enhet strengt fortrolig`() {
        saksbehandlerKontekst()

        val ident = Random.nextLong().toString()

        every { sakDao.finnSaker(any()) } returns
            listOf(
                Sak(
                    ident = ident,
                    sakType = SakType.BARNEPENSJON,
                    id = Random.nextLong(),
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                ),
            )

        assertThrows<ManglerTilgangTilEnhet> {
            service.hentEnkeltSakForPerson(ident)
        }

        verify { sakDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak strengt fortrolig - Bruker har sak`() {
        saksbehandlerKontekst(nasjonalTilgang = false, strentFortrolig = true)

        val ident = Random.nextLong().toString()

        val sak =
            Sak(
                ident = ident,
                sakType = SakType.BARNEPENSJON,
                id = Random.nextLong(),
                enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
            )
        every { sakDao.finnSaker(any()) } returns
            listOf(
                sak,
            )

        val enkeltsak = service.hentEnkeltSakForPerson(ident)

        enkeltsak shouldBe sak

        verify { sakDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har sak, og saksbehandler har tilgang til enhet Porsgrunn`() {
        saksbehandlerKontekst()

        val ident = Random.nextLong().toString()
        val enhet = Enheter.PORSGRUNN
        val sak =
            Sak(
                ident = ident,
                sakType = SakType.BARNEPENSJON,
                id = Random.nextLong(),
                enhet = enhet.enhetNr,
            )

        every { sakDao.finnSaker(any()) } returns listOf(sak)

        val enkeltsak = service.hentEnkeltSakForPerson(ident)

        enkeltsak shouldBe sak

        verify { sakDao.finnSaker(ident) }
    }
}
