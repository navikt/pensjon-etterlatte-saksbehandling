package no.nav.etterlatte.sak

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.ResponseException
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.LITE_BARN
import no.nav.etterlatte.PdltjenesterKlientTest
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.behandling.BrukerServiceImpl
import no.nav.etterlatte.behandling.IngenEnhetFunnetException
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.SkjermingKlientImpl
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.krr.KrrKlient
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.PdlFolkeregisterIdentListe
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import no.nav.etterlatte.saksbehandler.SaksbehandlerService
import no.nav.etterlatte.tilgangsstyring.AzureGroup
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SakServiceTest {
    private val pdlTjenesterKlient = spyk<PdltjenesterKlientTest>()
    private val norg2Klient = mockk<Norg2Klient>()
    private val brukerService = BrukerServiceImpl(pdlTjenesterKlient, norg2Klient)
    private val saksbehandlerService = mockk<SaksbehandlerService>()
    private val skjermingKlient = mockk<SkjermingKlientImpl>()
    private val sakSkrivDao = mockk<SakSkrivDao>()
    private val sakLesDao = mockk<SakLesDao>()
    private val sakendringerDao = mockk<SakendringerDao>()
    private val grunnlagservice = mockk<GrunnlagService>()
    private val krrKlient = mockk<KrrKlient>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val oppdaterTilgangService = mockk<OppdaterTilgangService>()
    private val saktilgang = mockk<SakTilgang>(relaxed = true)
    private val service: SakService =
        SakServiceImpl(
            sakSkrivDao,
            sakLesDao,
            sakendringerDao,
            skjermingKlient,
            brukerService,
            grunnlagservice,
            krrKlient,
            pdlTjenesterKlient,
            featureToggleService,
            oppdaterTilgangService,
            saktilgang,
            mockk(),
        )

    @BeforeEach
    fun before() {
        clearAllMocks()
        /*
         Asserter her på at vi må bruke systembruker siden vi ikke nødvendigvis har tilgang med saksbehandlertoken da alt skjer i en transaction
         Gjelder ikke for oppdaterIdentForSak()
         */
        coEvery { grunnlagservice.opprettGrunnlag(any(), any()) } just runs
        every { grunnlagservice.hentPersongalleri(any(SakId::class)) } returns null
        every { grunnlagservice.lagreNyeSaksopplysningerBareSak(any(), any()) } just runs

        coEvery { krrKlient.hentDigitalKontaktinformasjon(any()) } returns
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

    @AfterEach
    fun after() {
        confirmVerified(sakSkrivDao, pdlTjenesterKlient, norg2Klient)
    }

    private fun saksbehandlerKontekst(
        nasjonalTilgang: Boolean = false,
        strentFortrolig: Boolean = false,
        egenAnsatt: Boolean = false,
    ): Saksbehandler {
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
            spyk(
                SaksbehandlerMedEnheterOgRoller(
                    tokenValidationContext,
                    saksbehandlerService,
                    SaksbehandlerMedRoller(
                        saksbehandler,
                        groups,
                    ),
                    saksbehandler,
                ),
            ).also { every { it.name() } returns this::class.java.simpleName },
        )
        return saksbehandler
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
            spyk(
                SystemUser(
                    tokenValidationContext,
                    brukerTokenInfo,
                ),
            ).also { every { it.name() } returns this::class.java.simpleName },
        )
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler`() {
        saksbehandlerKontekst()

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        coEvery { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(enhetsNummer = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
            )

        every { sakLesDao.finnSaker(KONTANT_FOT.value) } returns
            listOf(
                Sak(
                    id = sakId1,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                    adressebeskyttelse = null,
                    erSkjermet = false,
                ),
            )

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 1

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `enhet filtrering skjer hvis vi har en saksbehandler uten riktig enhet`() {
        saksbehandlerKontekst()

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        coEvery { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(enhetsNummer = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
            )

        every { sakLesDao.finnSaker(KONTANT_FOT.value) } returns
            listOf(
                Sak(
                    id = sakId1,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                    adressebeskyttelse = null,
                    erSkjermet = false,
                ),
            )

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 0

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `enhet filtrering skjer ikke hvis vi har en system bruker`() {
        systemBrukerKontekst()

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        coEvery { saksbehandlerService.hentEnheterForSaksbehandlerIdentWrapper(any()) } returns
            listOf(
                SaksbehandlerEnhet(enhetsNummer = Enheter.PORSGRUNN.enhetNr, navn = Enheter.PORSGRUNN.navn),
            )

        every { sakLesDao.finnSaker(KONTANT_FOT.value) } returns
            listOf(
                Sak(
                    id = sakId1,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                    adressebeskyttelse = null,
                    erSkjermet = false,
                ),
            )

        val saker = service.finnSaker(KONTANT_FOT.value)

        saker.size shouldBe 1

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value) }
    }

    @Test
    fun `finn OMS sak for ident sak sin ident`() {
        saksbehandlerKontekst()
        val sakId = sakId1

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        every { grunnlagservice.hentSakerOgRoller(KONTANT_FOT) } returns
            PersonMedSakerOgRoller(
                KONTANT_FOT.value,
                emptyList(),
            )
        every { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD) } returns
            listOf(
                Sak(
                    id = sakId,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                    adressebeskyttelse = null,
                    erSkjermet = false,
                ),
            )

        val finnSakerOmsOgHvisAvdoed = service.finnSakerOmsOgHvisAvdoed(KONTANT_FOT.value)

        finnSakerOmsOgHvisAvdoed shouldContainExactly listOf(sakId)

        verify { grunnlagservice.hentSakerOgRoller(KONTANT_FOT) }
        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD) }
    }

    @Test
    fun `finn OMS sak for avdød i persongalleri på sak i finnSakerOmsOgHvisAvdoed`() {
        saksbehandlerKontekst()
        val sakId = sakId1

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        every { grunnlagservice.hentSakerOgRoller(KONTANT_FOT) } returns
            PersonMedSakerOgRoller(
                KONTANT_FOT.value,
                listOf(
                    SakidOgRolle(sakId, Saksrolle.AVDOED),
                ),
            )
        every { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD) } returns emptyList()

        val finnSakerOmsOgHvisAvdoed = service.finnSakerOmsOgHvisAvdoed(KONTANT_FOT.value)

        finnSakerOmsOgHvisAvdoed shouldContainExactly listOf(sakId)

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) {
            sakLesDao.finnSaker(KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD)
            grunnlagservice.hentSakerOgRoller(KONTANT_FOT)
        }
    }

    @Test
    fun `finnEllerOpprettSak feiler hvis PDL ikke finner geografisk tilknytning`() {
        val responseException = ResponseException(mockk(), "Oops")

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        every { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
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

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify {
            listOf(norg2Klient) wasNot Called
        }
    }

    @Test
    fun `finnEllerOpprettSak feiler hvis NORG2 ikke finner geografisk tilknytning`() {
        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        every {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(SakType.BARNEPENSJON.tema, "0301"),
            )
        } returns emptyList()
        every { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()

        val thrown =
            assertThrows<IngenEnhetFunnetException> {
                service.finnEllerOpprettSakMedGrunnlag(KONTANT_FOT.value, SakType.BARNEPENSJON)
            }

        thrown.arbeidsFordelingRequest.tema shouldBe SakType.BARNEPENSJON.tema
        thrown.arbeidsFordelingRequest.geografiskOmraade shouldBe "0301"

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(
                    SakType.BARNEPENSJON.tema,
                    "0301",
                ),
            )
        }
    }

    @Test
    fun `finnEllerOpprettSak lagre enhet hvis enhet er funnet`() {
        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        every { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
        val sak1 =
            Sak(
                id = sakId1,
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.PORSGRUNN.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )
        every { sakLesDao.hentSak(sakId1) } returns sak1
        every {
            sakSkrivDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        } returns
            sak1
        coEvery { skjermingKlient.personErSkjermet(KONTANT_FOT.value) } returns false
        every { sakSkrivDao.oppdaterSkjerming(any(), any()) } just runs
        every { sakSkrivDao.oppdaterAdresseBeskyttelse(sakId1, AdressebeskyttelseGradering.UGRADERT) } just runs
        every { grunnlagservice.grunnlagFinnesForSak(any()) } returns true
        every {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(
                    SakType.BARNEPENSJON.tema,
                    "0301",
                ),
            )
        } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.PORSGRUNN.navn, Enheter.PORSGRUNN.enhetNr),
            )

        val sak = service.finnEllerOpprettSakMedGrunnlag(KONTANT_FOT.value, SakType.BARNEPENSJON)

        sak shouldBe
            sak1

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakSkrivDao.oppdaterSkjerming(any(), any()) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        coVerify(exactly = 1) {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(KONTANT_FOT.value),
                    SakType.BARNEPENSJON,
                ),
            )
        }
        verify(exactly = 1) {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(
                    SakType.BARNEPENSJON.tema,
                    "0301",
                ),
            )
        }
        verify(exactly = 1) {
            sakSkrivDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        }
        verify(exactly = 1) { saktilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT) }
    }

    @Test
    fun `finnEllerOpprettSak lagrer enhet og setter gradering`() {
        systemBrukerKontekst()

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        every { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
        coEvery { skjermingKlient.personErSkjermet(KONTANT_FOT.value) } returns false
        every { grunnlagservice.grunnlagFinnesForSak(any()) } returns true
        every { sakSkrivDao.oppdaterSkjerming(any(), any()) } just runs
        val sak1 =
            Sak(
                id = sakId1,
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.PORSGRUNN.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )
        every {
            sakSkrivDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        } returns
            sak1
        every { sakLesDao.hentSak(sakId1) } returns sak1

        coEvery {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(PersonIdent(KONTANT_FOT.value), SakType.BARNEPENSJON),
            )
        } returns AdressebeskyttelseGradering.STRENGT_FORTROLIG

        every {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(
                    SakType.BARNEPENSJON.tema,
                    "0301",
                ),
            )
        } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.PORSGRUNN.navn, Enheter.PORSGRUNN.enhetNr),
            )
        every { sakSkrivDao.oppdaterAdresseBeskyttelse(any(), any()) } just runs
        every { sakSkrivDao.oppdaterEnhet(any()) } just runs

        val sak =
            service.finnEllerOpprettSakMedGrunnlag(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
            )

        sak shouldBe
            sak1

        verify(exactly = 1) {
            saktilgang.oppdaterAdressebeskyttelse(
                sak.id,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
            )
        }
        verify(exactly = 1) { sakSkrivDao.oppdaterSkjerming(any(), any()) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        coVerify(exactly = 1) {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(KONTANT_FOT.value),
                    SakType.BARNEPENSJON,
                ),
            )
        }
        verify(exactly = 1) {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(
                    SakType.BARNEPENSJON.tema,
                    "0301",
                ),
            )
        }
        verify(exactly = 1) {
            sakSkrivDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.PORSGRUNN.enhetNr)
        }
    }

    @Test
    fun `skal sette skjerming hvis skjermingstjenesten sier at person er skjermet`() {
        saksbehandlerKontekst()

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(KONTANT_FOT.value)
        every { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) } returns emptyList()
        val opprettetSak =
            Sak(
                id = sakId1,
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )
        every {
            sakSkrivDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr)
        } returns opprettetSak
        every { sakLesDao.hentSak(opprettetSak.id) } returns opprettetSak
        coEvery { skjermingKlient.personErSkjermet(KONTANT_FOT.value) } returns true
        every { sakSkrivDao.oppdaterEnhet(any()) } just runs
        every { sakSkrivDao.oppdaterAdresseBeskyttelse(sakId1, AdressebeskyttelseGradering.UGRADERT) } just runs
        every { sakSkrivDao.oppdaterSkjerming(any(), any()) } just runs
        every { grunnlagservice.grunnlagFinnesForSak(any()) } returns true

        every {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(
                    SakType.BARNEPENSJON.tema,
                    "0301",
                ),
            )
        } returns
            listOf(
                ArbeidsFordelingEnhet(Enheter.EGNE_ANSATTE.navn, Enheter.EGNE_ANSATTE.enhetNr),
            )

        val sak = service.finnEllerOpprettSakMedGrunnlag(KONTANT_FOT.value, SakType.BARNEPENSJON)

        sak shouldBe
            Sak(
                ident = KONTANT_FOT.value,
                sakType = SakType.BARNEPENSJON,
                id = sakId1,
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(KONTANT_FOT.value) }
        verify(exactly = 1) { sakSkrivDao.oppdaterSkjerming(any(), any()) }
        verify(exactly = 1) { sakLesDao.finnSaker(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        verify(exactly = 1) { pdlTjenesterKlient.hentGeografiskTilknytning(KONTANT_FOT.value, SakType.BARNEPENSJON) }
        coVerify(exactly = 1) {
            pdlTjenesterKlient.hentAdressebeskyttelseForPerson(
                HentAdressebeskyttelseRequest(
                    PersonIdent(KONTANT_FOT.value),
                    SakType.BARNEPENSJON,
                ),
            )
        }
        verify(exactly = 1) {
            norg2Klient.hentArbeidsfordelingForOmraadeOgTema(
                ArbeidsFordelingRequest(
                    SakType.BARNEPENSJON.tema,
                    "0301",
                ),
            )
        }
        verify(exactly = 1) {
            sakSkrivDao.opprettSak(KONTANT_FOT.value, SakType.BARNEPENSJON, Enheter.EGNE_ANSATTE.enhetNr)
        }
        verify(exactly = 1) { sakSkrivDao.oppdaterEnhet(any()) }
        verify(exactly = 1) { saktilgang.oppdaterAdressebeskyttelse(sak.id, AdressebeskyttelseGradering.UGRADERT) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har ingen saker`() {
        val ident = KONTANT_FOT.value

        saksbehandlerKontekst()

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(ident)
        every { sakLesDao.finnSaker(any()) } returns emptyList()

        assertThrows<PersonManglerSak> {
            service.hentSakHvisSaksbehandlerHarTilgang(ident)
        }

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident) }
        verify { sakLesDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har sak, men saksbehandler mangler tilgang til enhet egne ansatte`() {
        saksbehandlerKontekst()

        val ident = LITE_BARN.value

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(ident)
        every { sakLesDao.finnSaker(any()) } returns
            listOf(
                Sak(
                    ident = ident,
                    sakType = SakType.BARNEPENSJON,
                    id = randomSakId(),
                    enhet = Enheter.EGNE_ANSATTE.enhetNr,
                    adressebeskyttelse = null,
                    erSkjermet = false,
                ),
            )

        assertThrows<ManglerTilgangTilEnhet> {
            service.hentSakHvisSaksbehandlerHarTilgang(ident)
        }

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident) }
        verify { sakLesDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak egen ansatt - Bruker har sak egen ansatt`() {
        saksbehandlerKontekst(egenAnsatt = true)

        val ident = LITE_BARN.value

        val sak =
            Sak(
                ident = ident,
                sakType = SakType.BARNEPENSJON,
                id = randomSakId(),
                enhet = Enheter.EGNE_ANSATTE.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(ident)
        every { sakLesDao.finnSaker(any()) } returns
            listOf(
                sak,
            )

        val enkeltsak = service.hentSakHvisSaksbehandlerHarTilgang(ident)

        enkeltsak shouldBe sak

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident) }
        verify { sakLesDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har sak, men saksbehandler mangler tilgang til enhet strengt fortrolig`() {
        saksbehandlerKontekst()

        val ident = KONTANT_FOT.value

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(ident)
        every { sakLesDao.finnSaker(any()) } returns
            listOf(
                Sak(
                    ident = ident,
                    sakType = SakType.BARNEPENSJON,
                    id = randomSakId(),
                    enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                    adressebeskyttelse = null,
                    erSkjermet = false,
                ),
            )

        assertThrows<ManglerTilgangTilEnhet> {
            service.hentSakHvisSaksbehandlerHarTilgang(ident)
        }

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident) }
        verify { sakLesDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak strengt fortrolig - Bruker har sak`() {
        saksbehandlerKontekst(nasjonalTilgang = false, strentFortrolig = true)

        val ident = JOVIAL_LAMA.value

        val sak =
            Sak(
                ident = ident,
                sakType = SakType.BARNEPENSJON,
                id = randomSakId(),
                enhet = Enheter.STRENGT_FORTROLIG.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )
        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(ident)
        every { sakLesDao.finnSaker(any()) } returns
            listOf(
                sak,
            )

        val enkeltsak = service.hentSakHvisSaksbehandlerHarTilgang(ident)

        enkeltsak shouldBe sak

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident) }
        verify(exactly = 1) { sakLesDao.finnSaker(ident) }
    }

    @Test
    fun `Hent enkeltsak - Bruker har sak, og saksbehandler har tilgang til enhet Porsgrunn`() {
        saksbehandlerKontekst()

        val ident = LITE_BARN.value
        val enhet = Enheter.PORSGRUNN
        val sak =
            Sak(
                ident = ident,
                sakType = SakType.BARNEPENSJON,
                id = randomSakId(),
                enhet = enhet.enhetNr,
                adressebeskyttelse = null,
                erSkjermet = false,
            )

        coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns dummyPdlResponse(ident)
        every { sakLesDao.finnSaker(any()) } returns listOf(sak)

        val enkeltsak = service.hentSakHvisSaksbehandlerHarTilgang(ident)

        enkeltsak shouldBe sak

        coVerify(exactly = 1) { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident) }
        verify { sakLesDao.finnSaker(ident) }
    }

    @Nested
    inner class FlereIdenter {
        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Bruker har historisk folkeregisterident - bruker har én sak på historisk ident`(sakType: SakType) {
            saksbehandlerKontekst()

            val ident1 = LITE_BARN.value
            val ident2 = KONTANT_FOT.value

            coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns
                PdlFolkeregisterIdentListe(
                    listOf(
                        PdlIdentifikator.FolkeregisterIdent(LITE_BARN, true),
                        PdlIdentifikator.FolkeregisterIdent(KONTANT_FOT, false),
                    ),
                )
            every { sakLesDao.finnSaker(ident1, sakType) } returns listOf(dummySak(ident1, sakType))
            every { sakLesDao.finnSaker(ident2, sakType) } returns emptyList()

            service.finnSak(ident1, sakType)

            coVerify(exactly = 1) {
                pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident1)
            }
            verify(exactly = 1) {
                sakLesDao.finnSaker(ident1, sakType)
                sakLesDao.finnSaker(ident2, sakType)
            }
        }

        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Bruker har historisk folkeregisterident - bruker har flere saker av samme type`(sakType: SakType) {
            val ident1 = LITE_BARN.value
            val ident2 = KONTANT_FOT.value

            coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns
                PdlFolkeregisterIdentListe(
                    listOf(
                        PdlIdentifikator.FolkeregisterIdent(LITE_BARN, true),
                        PdlIdentifikator.FolkeregisterIdent(KONTANT_FOT, false),
                    ),
                )

            every { sakLesDao.finnSaker(ident1, sakType) } returns listOf(dummySak(ident1, sakType))
            every { sakLesDao.finnSaker(ident2, sakType) } returns listOf(dummySak(ident2, sakType))

            assertThrows<InternfeilException> {
                service.finnSak(ident1, sakType)
            }

            coVerify(exactly = 1) {
                pdlTjenesterKlient.hentPdlFolkeregisterIdenter(ident1)
            }
            verify(exactly = 1) {
                sakLesDao.finnSaker(ident1, sakType)
                sakLesDao.finnSaker(ident2, sakType)
            }
        }
    }

    @Nested
    inner class TestOppdaterIdentForSak {
        @Test
        fun `Sak ident ikke funnet i PDL`() {
            val sak = dummySak(ident = KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD)

            coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns
                PdlFolkeregisterIdentListe(
                    emptyList(),
                )

            assertThrows<InternfeilException> {
                service.oppdaterIdentForSak(sak, simpleSaksbehandler())
            }

            coVerify(exactly = 1) {
                pdlTjenesterKlient.hentPdlFolkeregisterIdenter(sak.ident)
            }

            verify {
                sakLesDao wasNot Called
                sakSkrivDao wasNot Called
            }
        }

        @Test
        fun `Bruker har flere gyldige identer i PDL`() {
            val sak = dummySak(ident = KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD)

            coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns
                PdlFolkeregisterIdentListe(
                    listOf(
                        PdlIdentifikator.FolkeregisterIdent(KONTANT_FOT),
                        PdlIdentifikator.FolkeregisterIdent(JOVIAL_LAMA),
                    ),
                )

            assertThrows<InternfeilException> {
                service.oppdaterIdentForSak(sak, simpleSaksbehandler())
            }

            coVerify(exactly = 1) {
                pdlTjenesterKlient.hentPdlFolkeregisterIdenter(sak.ident)
            }

            verify {
                sakLesDao wasNot Called
                sakSkrivDao wasNot Called
            }
        }

        @Test
        fun `Bruker har historisk ident`() {
            val saksbehandler = saksbehandlerKontekst()

            val sak = dummySak(ident = KONTANT_FOT.value, SakType.OMSTILLINGSSTOENAD)

            val persongalleri = Persongalleri("soeker", "innsender", listOf("soesken"), listOf("avdoed"))

            coEvery { pdlTjenesterKlient.hentPdlFolkeregisterIdenter(any()) } returns
                PdlFolkeregisterIdentListe(
                    listOf(
                        PdlIdentifikator.FolkeregisterIdent(KONTANT_FOT, true),
                        PdlIdentifikator.FolkeregisterIdent(JOVIAL_LAMA, false),
                    ),
                )
            justRun { sakSkrivDao.oppdaterIdent(any(), any()) }
            every { sakLesDao.hentSak(any()) } returns sak.copy(ident = JOVIAL_LAMA.value)
            every { grunnlagservice.hentPersongalleri(any<SakId>()) } returns persongalleri
            justRun { grunnlagservice.lagreNyeSaksopplysningerBareSak(any(), any()) }
            coJustRun { grunnlagservice.opprettEllerOppdaterGrunnlagForSak(any(), any()) }

            val oppdatertSak = service.oppdaterIdentForSak(sak, saksbehandler)

            oppdatertSak.id shouldBe sak.id
            oppdatertSak.enhet shouldBe sak.enhet
            oppdatertSak.sakType shouldBe sak.sakType

            oppdatertSak.ident shouldNotBe sak.ident
            oppdatertSak.ident shouldBe JOVIAL_LAMA.value

            coVerify(exactly = 1) {
                pdlTjenesterKlient.hentPdlFolkeregisterIdenter(sak.ident)
                grunnlagservice.opprettEllerOppdaterGrunnlagForSak(
                    sak.id,
                    match { it.persongalleri.soeker == oppdatertSak.ident },
                )
            }

            verify(exactly = 1) {
                sakSkrivDao.oppdaterIdent(sak.id, JOVIAL_LAMA)
                sakLesDao.hentSak(sak.id)
            }
        }
    }

    private fun dummySak(
        ident: String,
        sakType: SakType,
        enhet: Enheter = Enheter.PORSGRUNN,
        adressebeskyttelse: AdressebeskyttelseGradering? = null,
        erSkjermet: Boolean = false,
    ) = Sak(
        ident = ident,
        sakType = sakType,
        id = randomSakId(),
        enhet = enhet.enhetNr,
        adressebeskyttelse = adressebeskyttelse,
        erSkjermet = erSkjermet,
    )

    private fun dummyPdlResponse(ident: String) =
        PdlFolkeregisterIdentListe(
            identifikatorer =
                listOf(
                    PdlIdentifikator.FolkeregisterIdent(
                        folkeregisterident = Folkeregisteridentifikator.of(ident),
                    ),
                ),
        )
}
