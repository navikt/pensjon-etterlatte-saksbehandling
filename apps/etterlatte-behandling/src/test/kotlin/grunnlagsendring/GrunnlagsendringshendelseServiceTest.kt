package no.nav.etterlatte.grunnlagsendring

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.User
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.Folkeregisteridentifikatorhendelse
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagsendringshendelseServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao>(relaxUnitFun = true)
    private val pdlService = mockk<PdlTjenesterKlientImpl>()
    private val grunnlagService = mockk<GrunnlagService>()
    private val sakService = mockk<SakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val doedshendelseService = mockk<DoedshendelseService>()
    private val oppdaterTilgangService = mockk<OppdaterTilgangService>()
    private val grunnlagsendringsHendelseFilter = mockk<GrunnlagsendringsHendelseFilter>()
    private val mockOppgave =
        opprettNyOppgaveMedReferanseOgSak(
            "hendelseid",
            Sak("ident", SakType.BARNEPENSJON, sakId1, Enheter.AALESUND.enhetNr, null, false),
            OppgaveKilde.HENDELSE,
            OppgaveType.VURDER_KONSEKVENS,
            null,
        )

    private lateinit var grunnlagsendringshendelseService: GrunnlagsendringshendelseService
    private val tomtGrunnlag = Grunnlag.empty()

    @BeforeEach
    fun before() {
        nyKontekstMedBruker(mockk<User>().also { every { it.name() } returns this::class.java.simpleName })

        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns tomtGrunnlag

        grunnlagsendringshendelseService =
            spyk(
                GrunnlagsendringshendelseService(
                    oppgaveService,
                    grunnlagshendelsesDao,
                    behandlingService,
                    pdlService,
                    grunnlagService,
                    sakService,
                    doedshendelseService.apply {
                        every { opprettDoedshendelseForBeroertePersoner(any()) } returns Unit
                    },
                    grunnlagsendringsHendelseFilter.apply {
                        every { hendelseErRelevantForSak(any(), any()) } returns true
                    },
                    oppdaterTilgangService,
                ),
            )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `Sjekk at fnr matcher hendelse fnr og ikke sak ident i duplikatsjekk`() {
        val sakId = sakId1
        val sak = Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.STEINKJER.enhetNr, null, false)
        val grlhendelse =
            grunnlagsendringshendelseMedSamsvar(
                gjelderPerson = KONTANT_FOT.value,
                hendelseGjelderRolle = Saksrolle.GJENLEVENDE,
                samsvarMellomKildeOgGrunnlag = null,
            ).copy(
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                type = GrunnlagsendringsType.BOSTED,
            )

        val adresse =
            Adresse(
                type = AdresseType.VEGADRESSE,
                aktiv = true,
                kilde = "FREG",
                postnr = "2040",
                adresseLinje1 = "Furukollveien 189",
            )
        val samsvarBostedAdresse =
            SamsvarMellomKildeOgGrunnlag.Adresse(
                samsvar = false,
                fraPdl = listOf(adresse),
                fraGrunnlag = null,
                aarsakIgnorert = null,
            )

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns
            listOf(
                grlhendelse.copy(
                    id = randomUUID(),
                    samsvarMellomKildeOgGrunnlag = samsvarBostedAdresse,
                    gjelderPerson = sak.ident,
                ),
            )
        val erDuplikatHvisGjelderPersonErSakident =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                grlhendelse,
                samsvarBostedAdresse,
            )
        assertTrue(erDuplikatHvisGjelderPersonErSakident)

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns
            listOf(
                grlhendelse.copy(
                    id = randomUUID(),
                    samsvarMellomKildeOgGrunnlag = samsvarBostedAdresse,
                    gjelderPerson = JOVIAL_LAMA.value,
                ),
            )
        val erIkkeDuplikatHvisGjelderPersonIkkeErSakident =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                grlhendelse,
                samsvarBostedAdresse,
            )
        assertFalse(erIkkeDuplikatHvisGjelderPersonIkkeErSakident)
    }

    @Test
    fun `Skal ignorere duplikate hendelser men ikke om det er 0 hendelser fra før av`() {
        val sakId = sakId1
        val adresse =
            Adresse(
                type = AdresseType.VEGADRESSE,
                aktiv = true,
                kilde = "FREG",
                postnr = "2040",
                adresseLinje1 = "Furukollveien 189",
            )
        val samsvarBostedAdresse =
            SamsvarMellomKildeOgGrunnlag.Adresse(
                samsvar = false,
                fraPdl = listOf(adresse),
                fraGrunnlag = null,
                aarsakIgnorert = null,
            )
        val grlhendelse =
            grunnlagsendringshendelseMedSamsvar(
                gjelderPerson = KONTANT_FOT.value,
                hendelseGjelderRolle = Saksrolle.GJENLEVENDE,
                samsvarMellomKildeOgGrunnlag = null,
            ).copy(
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                type = GrunnlagsendringsType.BOSTED,
            )

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns emptyList()

        val erIkkeDuplikat =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                grlhendelse,
                samsvarBostedAdresse,
            )

        assertFalse(erIkkeDuplikat)

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns listOf(grlhendelse.copy(id = randomUUID(), samsvarMellomKildeOgGrunnlag = samsvarBostedAdresse))

        val erDuplikat =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                grlhendelse,
                samsvarBostedAdresse,
            )

        assertTrue(erDuplikat)
    }

    @Test
    fun `Skal ignorere duplikate hendelser ulikt samsvar adresse`() {
        val sakId = sakId1

        val adresse =
            Adresse(
                type = AdresseType.VEGADRESSE,
                aktiv = true,
                kilde = "FREG",
                postnr = "2040",
                adresseLinje1 = "Furukollveien 189",
            )
        val samsvarBostedAdresse =
            SamsvarMellomKildeOgGrunnlag.Adresse(
                samsvar = false,
                fraPdl = listOf(adresse),
                fraGrunnlag = null,
                aarsakIgnorert = null,
            )
        val grlhendelse =
            grunnlagsendringshendelseMedSamsvar(
                gjelderPerson = KONTANT_FOT.value,
                samsvarMellomKildeOgGrunnlag = samsvarBostedAdresse,
            ).copy(
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                type = GrunnlagsendringsType.BOSTED,
                hendelseGjelderRolle = Saksrolle.SOESKEN,
            )
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns
            listOf(
                grlhendelse,
            )
        val erDuplikat =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                grlhendelse,
                samsvarBostedAdresse,
            )
        assertTrue(erDuplikat)

        val erIkkeDuplikat =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                grlhendelse,
                samsvarBostedAdresse.copy(
                    fraPdl =
                        listOf(
                            Adresse(
                                type = AdresseType.VEGADRESSE,
                                aktiv = true,
                                kilde = "FREG",
                                postnr = "1359",
                                adresseLinje1 = "Trøgstadbåsta 612",
                            ),
                        ),
                ),
            )
        assertFalse(erIkkeDuplikat)
    }

    @Test
    fun `Skal opprette hendelser for hendelse - distinct på sakker og roller fra grunnlag`() {
        val sakId = sakId1
        val fnr = KONTANT_FOT.value

        every {
            sakService.finnSak(sakId)
        } returns Sak(fnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false)
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()
        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
        val sak2 = sakId2
        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    fnr,
                    listOf(
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.SOESKEN),
                        SakidOgRolle(sak2, Saksrolle.AVDOED),
                        SakidOgRolle(sak2, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sak2, Saksrolle.SOEKER),
                        SakidOgRolle(sakId3, Saksrolle.SOESKEN),
                    ),
                ),
            )

        every {
            sakService.finnSak(sak2)
        } returns Sak(fnr, SakType.BARNEPENSJON, sak2, Enheter.defaultEnhet.enhetNr, null, false)
        every {
            sakService.finnSak(sakId3)
        } returns Sak(fnr, SakType.BARNEPENSJON, sakId3, Enheter.defaultEnhet.enhetNr, null, false)

        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
        } returns grunnlagsendringshendelse

        every { pdlService.hentPdlModellForSaktype(any(), any(), SakType.BARNEPENSJON) } returns mockPerson()
        val opprettedeHendelser =
            grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(fnr, GrunnlagsendringsType.DOEDSFALL)
        assertEquals(6, opprettedeHendelser.size)
    }

    @ParameterizedTest
    @EnumSource(
        GrunnlagsendringsType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["INSTITUSJONSOPPHOLD", "SIVILSTAND", "UFOERETRYGD"],
    )
    fun `Gyldige hendelser for saktype BP`(grltype: GrunnlagsendringsType) {
        val soekerFnr = KONTANT_FOT.value
        val sakId = sakId1
        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    soekerFnr,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        val sak = Sak(soekerFnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false)
        every {
            sakService.finnSak(sakId)
        } returns sak
        every { pdlService.hentPdlModellForSaktype(soekerFnr, any(), sak.sakType) } returns
            mockPerson()
        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns emptyList()
        every { behandlingService.hentBehandlingerForSak(sak.id) } returns emptyList()
        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = soekerFnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(
                match {
                    it.type == grltype
                },
            )
        } returns grunnlagsendringshendelse

        val opprettetBostedHendelse =
            grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(
                soekerFnr,
                grltype,
            )
        assertTrue(opprettetBostedHendelse.isNotEmpty() && opprettetBostedHendelse.size == 1)
        assertEquals(grltype, opprettetBostedHendelse.first().type)
        assertEquals(Saksrolle.SOEKER, opprettetBostedHendelse.first().hendelseGjelderRolle)
    }

    @ParameterizedTest
    @EnumSource(
        GrunnlagsendringsType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["INSTITUSJONSOPPHOLD", "UFOERETRYGD"],
    )
    fun `Gyldige hendelser for saktype OMS`(grltype: GrunnlagsendringsType) {
        val soekerFnr = KONTANT_FOT.value
        val sakId = sakId1
        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    soekerFnr,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        val sak = Sak(soekerFnr, SakType.OMSTILLINGSSTOENAD, sakId, Enheter.defaultEnhet.enhetNr, null, false)
        every {
            sakService.finnSak(sakId)
        } returns sak
        every { pdlService.hentPdlModellForSaktype(soekerFnr, any(), sak.sakType) } returns
            mockPerson()
        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns emptyList()
        every { behandlingService.hentBehandlingerForSak(sak.id) } returns emptyList()
        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = soekerFnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(
                match {
                    it.type == grltype
                },
            )
        } returns grunnlagsendringshendelse

        val opprettetBostedHendelse =
            grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(
                soekerFnr,
                grltype,
            )
        assertTrue(opprettetBostedHendelse.isNotEmpty() && opprettetBostedHendelse.size == 1)
        assertEquals(grltype, opprettetBostedHendelse.first().type)
        assertEquals(Saksrolle.SOEKER, opprettetBostedHendelse.first().hendelseGjelderRolle)
    }

    @Test
    fun `Skal filtrere bort sivilstandshendelser for BP saker men ikke andre, OMS skal få de`() {
        val soekerFnr = KONTANT_FOT.value
        val sakId = sakId1
        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    soekerFnr,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        every {
            sakService.finnSak(sakId)
        } returns Sak(soekerFnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false)

        val tomSivilstandhendelserBP =
            grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(
                soekerFnr,
                GrunnlagsendringsType.SIVILSTAND,
            )
        assertTrue(tomSivilstandhendelserBP.isEmpty())

        every {
            sakService.finnSak(sakId)
        } returns Sak(soekerFnr, SakType.OMSTILLINGSSTOENAD, sakId, Enheter.defaultEnhet.enhetNr, null, false)

        every { pdlService.hentPdlModellForSaktype(soekerFnr, any(), SakType.OMSTILLINGSSTOENAD) } returns
            mockPerson()
        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns emptyList()
        val grlhendelse =
            grunnlagsendringshendelseMedSamsvar(
                gjelderPerson = soekerFnr,
                hendelseGjelderRolle = Saksrolle.SOEKER,
                samsvarMellomKildeOgGrunnlag = null,
            ).copy(
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                type = GrunnlagsendringsType.BOSTED,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grlhendelse
        val ikketomOmsHendelser =
            grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(
                soekerFnr,
                GrunnlagsendringsType.SIVILSTAND,
            )
        assertTrue(ikketomOmsHendelser.isNotEmpty())
    }

    @Test
    fun `Skal matche på hendelse fnr mot tidligere grl hendelser gjelder_person og ikke relatert sak sin ident men på egen`() {
        val sakId = sakId1
        val soekerFnr = KONTANT_FOT.value
        val gjenlevendeFnr = JOVIAL_LAMA.value
        val bostedAdresse =
            Adresse(
                type = AdresseType.VEGADRESSE,
                aktiv = true,
                kilde = "FREG",
                postnr = "2040",
                adresseLinje1 = "Furukollveien 189",
            )
        every { pdlService.hentPdlModellForSaktype(gjenlevendeFnr, any(), SakType.BARNEPENSJON) } returns
            mockPerson()
                .copy(bostedsadresse = listOf(OpplysningDTO(bostedAdresse, "adresse")))
        every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
        every { behandlingService.hentBehandlingerForSak(sakId) } returns emptyList()
        every {
            sakService.finnSak(sakId)
        } returns Sak(soekerFnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false)
        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    soekerFnr,
                    listOf(SakidOgRolle(sakId, Saksrolle.GJENLEVENDE)),
                ),
            )

        val samsvarBostedAdresse =
            SamsvarMellomKildeOgGrunnlag.Adresse(
                samsvar = false,
                fraPdl = listOf(bostedAdresse),
                fraGrunnlag = null,
                aarsakIgnorert = null,
            )

        val grlhendelse =
            grunnlagsendringshendelseMedSamsvar(
                gjelderPerson = gjenlevendeFnr,
                hendelseGjelderRolle = Saksrolle.GJENLEVENDE,
                samsvarMellomKildeOgGrunnlag = samsvarBostedAdresse,
            ).copy(
                status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                type = GrunnlagsendringsType.BOSTED,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(
                match {
                    it.status == GrunnlagsendringStatus.FORKASTET && it.samsvarMellomKildeOgGrunnlag == samsvarBostedAdresse
                },
            )
        } returns grlhendelse

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns listOf(grlhendelse)
        grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(gjenlevendeFnr, GrunnlagsendringsType.BOSTED)

        verify(
            exactly = 1,
        ) { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(match { it.status == GrunnlagsendringStatus.FORKASTET }) }
        verify(
            exactly = 0,
        ) { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(match { it.status != GrunnlagsendringStatus.FORKASTET }) }
    }

    @Test
    fun `Skal opprette doedshendelse for person og returnere oppgave`() {
        val grunnlagsendringshendelse =
            Grunnlagsendringshendelse(
                id = randomUUID(),
                sakId = sakId1,
                type = GrunnlagsendringsType.DOEDSFALL,
                gjelderPerson = KONTANT_FOT.value,
                samsvarMellomKildeOgGrunnlag = null,
                status = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                hendelseGjelderRolle = Saksrolle.SOEKER,
                opprettet = LocalDateTime.now(),
            )
        every { grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any()) } returns emptyList()
        every { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any()) } returns grunnlagsendringshendelse
        every {
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(
                hendelseId = grunnlagsendringshendelse.id,
                foerStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
                etterStatus = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                samsvarMellomKildeOgGrunnlag = any(),
            )
        } returns Unit
        every {
            oppgaveService.opprettOppgave(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockOppgave.copy(referanse = grunnlagsendringshendelse.id.toString())

        val oppgave = grunnlagsendringshendelseService.opprettDoedshendelseForPerson(grunnlagsendringshendelse)

        oppgave.id shouldBe mockOppgave.id
        oppgave.referanse shouldBe grunnlagsendringshendelse.id.toString()
    }

    @Test
    fun `Skal ikke gjøre oppdateringer om sakidene ikke finnes`() {
        val fnr = "16508201382"
        val adressebeskyttelse =
            no.nav.etterlatte.libs.common.pdlhendelse
                .Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr)

        every { grunnlagService.hentAlleSakerForFnr(any()) } returns emptySet()

        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
        }

        verify(exactly = 1) {
            grunnlagService.hentAlleSakerForFnr(Folkeregisteridentifikator.of(adressebeskyttelse.fnr))
        }
    }

    @Test
    fun `Skal gjøre tilgangsoppdateringer om sakider fines for hendelse adressebeskyttelse`() {
        val fnr = "16508201382"
        val adressebeskyttelse =
            no.nav.etterlatte.libs.common.pdlhendelse
                .Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr)

        val sakId = SakId(1L)
        every { grunnlagService.hentAlleSakerForFnr(any()) } returns setOf(sakId)
        val persongalleri = persongalleri()
        every { grunnlagService.hentPersongalleri(sakId) } returns persongalleri
        coEvery { oppdaterTilgangService.haandtergraderingOgEgenAnsatt(any(), any(), any()) } just Runs
        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
        }

        coVerify(exactly = 1) {
            oppdaterTilgangService.haandtergraderingOgEgenAnsatt(sakId, persongalleri, tomtGrunnlag)
        }
        verify { grunnlagService.hentAlleSakerForFnr(Folkeregisteridentifikator.of(adressebeskyttelse.fnr)) }
    }

    @Test
    fun `Skal opprette hendelse for ufoeretrygd hvis sak finnes og er loepende`() {
        val sakId = sakId1
        val fnr = KONTANT_FOT.value

        val hendelse =
            UfoereHendelse(
                personIdent = fnr,
                fodselsdato = LocalDate.of(2024, Month.APRIL, 1),
                virkningsdato = LocalDate.of(2024, Month.APRIL, 1),
                vedtaksType = VedtaksType.INNV,
            )

        every { sakService.finnSak(sakId) } returns
            Sak(fnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false)

        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    KONTANT_FOT.value,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        every { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any()) } returns mockk(relaxed = true)
        every {
            oppgaveService.opprettOppgave(
                any(),
                sakId,
                OppgaveKilde.HENDELSE,
                OppgaveType.VURDER_KONSEKVENS,
                any(),
                any(),
            )
        } returns
            mockk(relaxed = true)
        every {
            grunnlagsendringsHendelseFilter.hendelseErRelevantForSak(
                sakId,
                GrunnlagsendringsType.UFOERETRYGD,
            )
        } returns true

        val grunnlagsendringsshendelser = grunnlagsendringshendelseService.opprettUfoerehendelse(hendelse)

        grunnlagsendringsshendelser.shouldNotBeEmpty()
    }

    @Test
    fun `Skal ikke opprette hendelse for ufoeretrygd hvis sak finnes men ikke er loepende`() {
        val sakId = sakId1
        val fnr = KONTANT_FOT.value

        val hendelse =
            UfoereHendelse(
                personIdent = fnr,
                fodselsdato = LocalDate.of(2024, Month.APRIL, 1),
                virkningsdato = LocalDate.of(2024, Month.APRIL, 1),
                vedtaksType = VedtaksType.INNV,
            )

        every { sakService.finnSak(sakId) } returns
            Sak(fnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false)

        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    KONTANT_FOT.value,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        every {
            grunnlagsendringsHendelseFilter.hendelseErRelevantForSak(
                sakId,
                GrunnlagsendringsType.UFOERETRYGD,
            )
        } returns false

        val grunnlagsendringsshendelser = grunnlagsendringshendelseService.opprettUfoerehendelse(hendelse)

        grunnlagsendringsshendelser.shouldBeEmpty()
    }

    @Test
    fun `Skal ikke opprette hendelse for ufoeretrygd hvis sak ikke finnes`() {
        val sakId = sakId1
        val fnr = KONTANT_FOT.value

        val hendelse =
            UfoereHendelse(
                personIdent = fnr,
                fodselsdato = LocalDate.of(2024, Month.APRIL, 1),
                virkningsdato = LocalDate.of(2024, Month.APRIL, 1),
                vedtaksType = VedtaksType.INNV,
            )

        every { sakService.finnSak(sakId) } returns null

        every { grunnlagService.hentSakerOgRoller(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    KONTANT_FOT.value,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        val grunnlagsendringsshendelser = grunnlagsendringshendelseService.opprettUfoerehendelse(hendelse)

        grunnlagsendringsshendelser.shouldBeEmpty()
    }

    @Nested
    inner class TestHendelserFolkeregisterident {
        @Test
        fun `Håndter hendelse for endret folkeregisteridentifikator - bruker har ingen saker`() {
            every { sakService.finnSaker(any()) } returns emptyList()

            val hendelse =
                Folkeregisteridentifikatorhendelse(
                    hendelseId = randomUUID().toString(),
                    endringstype = Endringstype.KORRIGERT,
                    fnr = KONTANT_FOT.value,
                    gammeltFnr = null,
                )

            grunnlagsendringshendelseService.opprettFolkeregisteridentifikatorhendelse(hendelse)

            coVerify { grunnlagService wasNot Called }
            verify {
                sakService.finnSaker(hendelse.fnr)
                pdlService wasNot Called
                grunnlagshendelsesDao wasNot Called
            }
        }

        @Test
        fun `Håndter hendelse for endret folkeregisteridentifikator - OMS`() {
            val sak =
                Sak(
                    JOVIAL_LAMA.value,
                    SakType.OMSTILLINGSSTOENAD,
                    SakId(1L),
                    Enheter.defaultEnhet.enhetNr,
                    null,
                    false,
                )

            every { sakService.finnSaker(any()) } returns listOf(sak)
            every { pdlService.hentPdlModellForSaktype(any(), any(), SakType.OMSTILLINGSSTOENAD) } returns mockPerson()
            every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
            every {
                grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
            } returns emptyList()
            every { behandlingService.hentBehandlingerForSak(any()) } returns
                listOf(
                    mockk<Behandling> { every { status } returns BehandlingStatus.OPPRETTET },
                )
            every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns mockk(relaxed = true)

            val grunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()
            every { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(grunnlagsendringshendelse)) } returns mockk()

            val hendelse =
                Folkeregisteridentifikatorhendelse(
                    hendelseId = randomUUID().toString(),
                    endringstype = Endringstype.KORRIGERT,
                    fnr = KONTANT_FOT.value,
                    gammeltFnr = null,
                )

            grunnlagsendringshendelseService.opprettFolkeregisteridentifikatorhendelse(hendelse)

            with(grunnlagsendringshendelse.captured) {
                assertEquals(sak.id, this.sakId)
                assertEquals(sak.ident, this.gjelderPerson)
                assertEquals(GrunnlagsendringsType.FOLKEREGISTERIDENTIFIKATOR, this.type)
                assertEquals(GrunnlagsendringStatus.SJEKKET_AV_JOBB, this.status)
                assertFalse(this.samsvarMellomKildeOgGrunnlag!!.samsvar)
                assertEquals(
                    hendelse.fnr,
                    (this.samsvarMellomKildeOgGrunnlag as SamsvarMellomKildeOgGrunnlag.Folkeregisteridentifikatorsamsvar).fraPdl!!.value,
                )
            }
            verify {
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)
                sakService.finnSaker(hendelse.fnr)
                pdlService.hentPdlModellForSaktype(sak.ident, PersonRolle.GJENLEVENDE, sak.sakType)
                grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sak.id, any())
                grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
                oppgaveService.opprettOppgave(
                    any(),
                    sak.id,
                    OppgaveKilde.HENDELSE,
                    OppgaveType.VURDER_KONSEKVENS,
                    any(),
                    null,
                    null,
                )
            }
        }

        @Test
        fun `Håndter hendelse for endret folkeregisteridentifikator - BP`() {
            val sak =
                Sak(
                    JOVIAL_LAMA.value,
                    SakType.BARNEPENSJON,
                    SakId(1L),
                    Enheter.defaultEnhet.enhetNr,
                    null,
                    false,
                )

            every { sakService.finnSaker(any()) } returns listOf(sak)
            every { pdlService.hentPdlModellForSaktype(any(), any(), SakType.BARNEPENSJON) } returns mockPerson()
            every { grunnlagService.hentOpplysningsgrunnlagForSak(any()) } returns Grunnlag.empty()
            every {
                grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
            } returns emptyList()
            every { behandlingService.hentBehandlingerForSak(any()) } returns
                listOf(
                    mockk<Behandling> { every { status } returns BehandlingStatus.OPPRETTET },
                )
            every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns mockk(relaxed = true)

            val grunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()
            every { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(grunnlagsendringshendelse)) } returns mockk()

            val hendelse =
                Folkeregisteridentifikatorhendelse(
                    hendelseId = randomUUID().toString(),
                    endringstype = Endringstype.KORRIGERT,
                    fnr = KONTANT_FOT.value,
                    gammeltFnr = null,
                )

            grunnlagsendringshendelseService.opprettFolkeregisteridentifikatorhendelse(hendelse)

            with(grunnlagsendringshendelse.captured) {
                assertEquals(sak.id, this.sakId)
                assertEquals(sak.ident, this.gjelderPerson)
                assertEquals(GrunnlagsendringsType.FOLKEREGISTERIDENTIFIKATOR, this.type)
                assertEquals(GrunnlagsendringStatus.SJEKKET_AV_JOBB, this.status)
                assertFalse(this.samsvarMellomKildeOgGrunnlag!!.samsvar)
                assertEquals(
                    hendelse.fnr,
                    (this.samsvarMellomKildeOgGrunnlag as SamsvarMellomKildeOgGrunnlag.Folkeregisteridentifikatorsamsvar).fraPdl!!.value,
                )
            }

            verify {
                grunnlagService.hentOpplysningsgrunnlagForSak(sak.id)
                sakService.finnSaker(hendelse.fnr)
                pdlService.hentPdlModellForSaktype(sak.ident, PersonRolle.BARN, sak.sakType)
                grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sak.id, any())
                grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
                oppgaveService.opprettOppgave(
                    any(),
                    sak.id,
                    OppgaveKilde.HENDELSE,
                    OppgaveType.VURDER_KONSEKVENS,
                    any(),
                    null,
                    null,
                )
            }
        }
    }
}
