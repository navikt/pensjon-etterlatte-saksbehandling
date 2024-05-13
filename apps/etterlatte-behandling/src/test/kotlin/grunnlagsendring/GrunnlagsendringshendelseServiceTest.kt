package no.nav.etterlatte.grunnlagsendring

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.JOVIAL_LAMA
import no.nav.etterlatte.KONTANT_FOT
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.behandling.domain.SamsvarMellomKildeOgGrunnlag
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseService
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.opprettNyOppgaveMedReferanseOgSak
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.nyKontekstMedBruker
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagsendringshendelseServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao>(relaxUnitFun = true)
    private val pdlService = mockk<PdlTjenesterKlientImpl>()
    private val grunnlagKlient = mockk<GrunnlagKlient>()
    private val sakService = mockk<SakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val brukerService = mockk<BrukerService>()
    private val doedshendelseService = mockk<DoedshendelseService>()
    private val mockOppgave =
        opprettNyOppgaveMedReferanseOgSak(
            "hendelseid",
            Sak("ident", SakType.BARNEPENSJON, 1L, Enheter.AALESUND.enhetNr),
            OppgaveKilde.HENDELSE,
            OppgaveType.VURDER_KONSEKVENS,
            null,
        )

    private lateinit var grunnlagsendringshendelseService: GrunnlagsendringshendelseService

    @BeforeEach
    fun before() {
        nyKontekstMedBruker(mockk())

        grunnlagsendringshendelseService =
            spyk(
                GrunnlagsendringshendelseService(
                    oppgaveService,
                    grunnlagshendelsesDao,
                    behandlingService,
                    pdlService,
                    grunnlagKlient,
                    sakService,
                    brukerService,
                    doedshendelseService.apply {
                        every { opprettDoedshendelseForBeroertePersoner(any()) } returns Unit
                        every { kanBrukeDeodshendelserJob() } returns false
                        every { kanSendeBrevOgOppretteOppgave() } returns false
                    },
                ),
            )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `Sjekk at fnr matcher hendelse fnr og ikke sak ident i duplikatsjekk`() {
        val sakId = 1L
        val sak = Sak(KONTANT_FOT.value, SakType.BARNEPENSJON, sakId, Enheter.STEINKJER.enhetNr)
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
            )

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(
                sakId,
                listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB),
            )
        } returns
            listOf(grlhendelse.copy(id = randomUUID(), samsvarMellomKildeOgGrunnlag = samsvarBostedAdresse, gjelderPerson = sak.ident))
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
        val sakId = 1L
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
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB))
        } returns emptyList()

        val erIkkeDuplikat =
            grunnlagsendringshendelseService.erDuplikatHendelse(
                sakId,
                grlhendelse,
                samsvarBostedAdresse,
            )

        assertFalse(erIkkeDuplikat)

        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB))
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
        val sakId = 1L

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
        val sakId = 1L
        val fnr = KONTANT_FOT.value

        every {
            sakService.finnSak(sakId)
        } returns Sak(fnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()
        coEvery { grunnlagKlient.hentGrunnlag(any()) } returns Grunnlag.empty()
        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    fnr,
                    listOf(
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(sakId, Saksrolle.SOESKEN),
                        SakidOgRolle(2L, Saksrolle.AVDOED),
                        SakidOgRolle(2L, Saksrolle.GJENLEVENDE),
                        SakidOgRolle(2L, Saksrolle.SOEKER),
                        SakidOgRolle(3L, Saksrolle.SOESKEN),
                    ),
                ),
            )

        every {
            sakService.finnSak(2L)
        } returns Sak(fnr, SakType.BARNEPENSJON, 2L, Enheter.defaultEnhet.enhetNr)
        every {
            sakService.finnSak(3L)
        } returns Sak(fnr, SakType.BARNEPENSJON, 3L, Enheter.defaultEnhet.enhetNr)

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

        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns mockPerson()
        val opprettedeHendelser =
            grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(fnr, GrunnlagsendringsType.DOEDSFALL)
        assertEquals(6, opprettedeHendelser.size)
    }

    @ParameterizedTest
    @EnumSource(
        GrunnlagsendringsType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["INSTITUSJONSOPPHOLD", "SIVILSTAND"],
    )
    fun `Gyldige hendelser for saktype BP`(grltype: GrunnlagsendringsType) {
        val soekerFnr = KONTANT_FOT.value
        val sakId = 1L
        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    soekerFnr,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        val sak = Sak(soekerFnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)
        every {
            sakService.finnSak(sakId)
        } returns sak
        every { pdlService.hentPdlModellFlereSaktyper(soekerFnr, any(), sak.sakType) } returns
            mockPerson()
        coEvery { grunnlagKlient.hentGrunnlag(any()) } returns Grunnlag.empty()
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB))
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
        names = ["INSTITUSJONSOPPHOLD"],
    )
    fun `Gyldige hendelser for saktype OMS`(grltype: GrunnlagsendringsType) {
        val soekerFnr = KONTANT_FOT.value
        val sakId = 1L
        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    soekerFnr,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        val sak = Sak(soekerFnr, SakType.OMSTILLINGSSTOENAD, sakId, Enheter.defaultEnhet.enhetNr)
        every {
            sakService.finnSak(sakId)
        } returns sak
        every { pdlService.hentPdlModellFlereSaktyper(soekerFnr, any(), sak.sakType) } returns
            mockPerson()
        coEvery { grunnlagKlient.hentGrunnlag(any()) } returns Grunnlag.empty()
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB))
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
        val sakId = 1L
        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
            .returns(
                PersonMedSakerOgRoller(
                    soekerFnr,
                    listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)),
                ),
            )

        every {
            sakService.finnSak(sakId)
        } returns Sak(soekerFnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)

        val tomSivilstandhendelserBP =
            grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(
                soekerFnr,
                GrunnlagsendringsType.SIVILSTAND,
            )
        assertTrue(tomSivilstandhendelserBP.isEmpty())

        every {
            sakService.finnSak(sakId)
        } returns Sak(soekerFnr, SakType.OMSTILLINGSSTOENAD, sakId, Enheter.defaultEnhet.enhetNr)

        every { pdlService.hentPdlModellFlereSaktyper(soekerFnr, any(), SakType.OMSTILLINGSSTOENAD) } returns
            mockPerson()
        coEvery { grunnlagKlient.hentGrunnlag(any()) } returns Grunnlag.empty()
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB))
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
        val sakId = 1L
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
        every { pdlService.hentPdlModellFlereSaktyper(gjenlevendeFnr, any(), SakType.BARNEPENSJON) } returns
            mockPerson()
                .copy(bostedsadresse = listOf(OpplysningDTO(bostedAdresse, "adresse")))
        coEvery { grunnlagKlient.hentGrunnlag(any()) } returns Grunnlag.empty()
        every { behandlingService.hentBehandlingerForSak(sakId) } returns emptyList()
        every {
            sakService.finnSak(sakId)
        } returns Sak(soekerFnr, SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr)
        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
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
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, listOf(GrunnlagsendringStatus.SJEKKET_AV_JOBB))
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
    fun `skal opprette grunnlagsendringshendelser i databasen for doedshendelser - base case`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val foerstegangsbehandlinger =
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT),
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.FATTET_VEDTAK),
            )
        val grunnlagsendringshendelse =
            grunnlagsendringshendelseMedSamsvar(
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()
        every { sakService.finnSak(sakId) } returns
            Sak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )
        every {
            grunnlagshendelsesDao.oppdaterGrunnlagsendringStatusOgSamsvar(any(), any(), any(), any())
        } returns Unit
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
        } returns grunnlagsendringshendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()

        every { behandlingService.hentBehandlingerForSak(sakId) } returns foerstegangsbehandlinger

        coEvery { grunnlagKlient.hentPersonSakOgRolle(any()) }
            .returns(PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER))))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(LocalDate.now(), "doedsdato"))

        val lagredeGrunnlagsendringshendelser =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                DoedshendelsePdl(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = LocalDate.of(2022, 1, 1),
                    endringstype = Endringstype.OPPRETTET,
                ),
            )
        // oppretter grunnlagshendringer i databasen for doedshendelser
        val opprettetHendelse = lagredeGrunnlagsendringshendelser.first()
        assertEquals(grunnlagsendringshendelse.gjelderPerson, opprettetHendelse.gjelderPerson)
        assertEquals(grunnlagsendringshendelse.type, opprettetHendelse.type)
        assertEquals(grunnlagsendringshendelse.status, opprettetHendelse.status)
        assertEquals(grunnlagsendringshendelse.hendelseGjelderRolle, opprettetHendelse.hendelseGjelderRolle)
        assertEquals(grunnlagsendringshendelse.samsvarMellomKildeOgGrunnlag, opprettetHendelse.samsvarMellomKildeOgGrunnlag)

        assertEquals(1, lagredeGrunnlagsendringshendelser.size)
        assertEquals(sakId, opprettGrunnlagsendringshendelse.captured.sakId)
        assertEquals(GrunnlagsendringsType.DOEDSFALL, opprettGrunnlagsendringshendelse.captured.type)
    }

    @Test
    fun `skal ikke opprette ny doedshendelse dersom en lignende allerede eksisterer - ny hendelse på x med lik info, har behandling`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)

        every { sakService.finnSak(sakId) } returns
            Sak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )

        coEvery {
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
            )
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket og da er ikke grunnlag null

        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        val doedsfallhendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )
        every { grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any()) } returns doedsfallhendelse

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList()
        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                DoedshendelsePdl(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = doedsdato,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns lagredeGrunnlagsendringshendelser1
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettRelevantHendelse(any(), any()) }
        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
        val lagretHendelse = lagredeGrunnlagsendringshendelser1.first()
        assertEquals(doedsfallhendelse.gjelderPerson, lagretHendelse.gjelderPerson)
        assertEquals(doedsfallhendelse.type, lagretHendelse.type)
        assertEquals(doedsfallhendelse.status, lagretHendelse.status)
        assertEquals(doedsfallhendelse.hendelseGjelderRolle, lagretHendelse.hendelseGjelderRolle)
        assertEquals(doedsfallhendelse.samsvarMellomKildeOgGrunnlag, lagretHendelse.samsvarMellomKildeOgGrunnlag)

        mockkStatic(Grunnlag::doedsdato)
        val grunnlagMock = mockk<Grunnlag>()
        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns grunnlagMock
        with(grunnlagMock) {
            every { doedsdato(any(), any()) } returns Opplysning.Konstant(randomUUID(), kilde, doedsdato)
        }

        grunnlagsendringshendelseService.opprettDoedshendelse(
            DoedshendelsePdl(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.ANNULLERT,
            ),
        )

        verify(exactly = 1) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
    }

    @Test
    fun `skal opprette ny doedshendelse med ny info dersom en allerede eksisterer - ny hendelse på x med ulik info, har behandling`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val doedsfallhendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns
            Sak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns doedsfallhendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                doedsfallhendelse,
            )
        coEvery {
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
            )
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket og da er ikke grunnlag null

        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()
        val lagredeGrunnlagsendringshendelser1 =
            grunnlagsendringshendelseService.opprettDoedshendelse(
                DoedshendelsePdl(
                    hendelseId = "1",
                    fnr = fnr,
                    doedsdato = doedsdato,
                    endringstype = Endringstype.OPPRETTET,
                ),
            )
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettRelevantHendelse(any(), any()) }
        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }

        val lagretHendelse = lagredeGrunnlagsendringshendelser1.first()
        assertEquals(doedsfallhendelse.gjelderPerson, lagretHendelse.gjelderPerson)
        assertEquals(doedsfallhendelse.type, lagretHendelse.type)
        assertEquals(doedsfallhendelse.status, lagretHendelse.status)
        assertEquals(doedsfallhendelse.hendelseGjelderRolle, lagretHendelse.hendelseGjelderRolle)
        assertEquals(doedsfallhendelse.samsvarMellomKildeOgGrunnlag, lagretHendelse.samsvarMellomKildeOgGrunnlag)

        val nyDoedsdato = LocalDate.of(2022, 8, 8)
        mockkStatic(Grunnlag::doedsdato)
        val grunnlagMock = mockk<Grunnlag>()
        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns grunnlagMock
        with(grunnlagMock) {
            every { doedsdato(any(), any()) } returns Opplysning.Konstant(randomUUID(), kilde, nyDoedsdato)
        }

        grunnlagsendringshendelseService.opprettDoedshendelse(
            DoedshendelsePdl(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = nyDoedsdato,
                endringstype = Endringstype.ANNULLERT,
            ),
        )

        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
        verify(exactly = 2) { grunnlagsendringshendelseService.opprettRelevantHendelse(any(), any()) }
    }

    @Test
    fun `skal ikke opprette ny doedshendelse hvis man ikke har gyldige behandlinger`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val doedsfallhendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns
            Sak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns doedsfallhendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                doedsfallhendelse,
            )
        coEvery {
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns emptyList()
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket

        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()

        grunnlagsendringshendelseService.opprettDoedshendelse(
            DoedshendelsePdl(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.OPPRETTET,
            ),
        )
        verify(exactly = 1) {
            grunnlagsendringshendelseService.opprettRelevantHendelse(match { it.type == GrunnlagsendringsType.DOEDSFALL }, any())
        }
        verify(
            exactly = 1,
        ) { grunnlagsendringshendelseService.forkastHendelse(match { it.type == GrunnlagsendringsType.DOEDSFALL }, any()) }
    }

    @Test
    fun `skal kun opprette ny doedshendelse for saker som finnes med gyldig behandling`() {
        val sakId = 1L
        val fnr = KONTANT_FOT.value
        val doedsdato = LocalDate.of(2022, 7, 8)
        val grunnlagsendringshendelse1 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.DOEDSFALL,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val grunnlagsendringshendelse2 =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.UTFLYTTING,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = fnr,
                samsvarMellomKildeOgGrunnlag = null,
            )

        every { sakService.finnSak(sakId) } returns
            Sak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(any())
        } returns grunnlagsendringshendelse1
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
        } returns emptyList() andThen
            listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2,
            )
        coEvery {
            grunnlagKlient.hentPersonSakOgRolle(any())
        } returns PersonMedSakerOgRoller(fnr, listOf(SakidOgRolle(sakId, Saksrolle.SOEKER)))
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave

        every { behandlingService.hentBehandlingerForSak(sakId) } returns
            listOf(
                foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.BEREGNET),
            )
        // Så hendelse ikke blir forkastet i oppdaterHendelseSjekket

        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns
            mockPerson()
                .copy(doedsdato = OpplysningDTO(doedsdato, "doedsdato"))

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()

        grunnlagsendringshendelseService.opprettDoedshendelse(
            DoedshendelsePdl(
                hendelseId = "1",
                fnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.OPPRETTET,
            ),
        )
        verify(exactly = 1) { grunnlagsendringshendelseService.opprettRelevantHendelse(any(), any()) }
        verify(exactly = 0) { grunnlagsendringshendelseService.forkastHendelse(any(), any()) }
    }

    @Test
    fun `Skal opprette doedshendelse for person og returnere oppgave`() {
        val grunnlagsendringshendelse =
            Grunnlagsendringshendelse(
                id = randomUUID(),
                sakId = 1,
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
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
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
    fun `Skal kunne sette adressebeskyttelse strengt fortrolig og sette enhet`() {
        val sakIder: Set<Long> = setOf(1, 2, 3, 4, 5, 6)
        val saker =
            sakIder.map {
                Sak(
                    id = it,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                )
            }
        val fnr = "16508201382"
        val adressebeskyttelse =
            Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr, AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        coEvery { grunnlagKlient.hentAlleSakIder(any()) } returns sakIder
        every { sakService.oppdaterAdressebeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(fnr) } returns saker
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } returns Unit
        every {
            brukerService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr)
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
        }

        coVerify(exactly = 1) { grunnlagKlient.hentAlleSakIder(adressebeskyttelse.fnr) }

        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns mockPerson()

        sakIder.forEach {
            verify(exactly = 1) {
                sakService.oppdaterAdressebeskyttelse(
                    it,
                    adressebeskyttelse.adressebeskyttelseGradering,
                )
            }
            verify(exactly = 1) {
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
            }
        }
    }

    @Test
    fun `Skal kunne sette fortrolig og sette enhet`() {
        val sakIder: Set<Long> = setOf(1, 2, 3, 4, 5, 6)
        val saker =
            sakIder.map {
                Sak(
                    id = it,
                    ident = KONTANT_FOT.value,
                    sakType = SakType.BARNEPENSJON,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                )
            }
        val fnr = AVDOED2_FOEDSELSNUMMER.value
        val adressebeskyttelse =
            Adressebeskyttelse("1", Endringstype.OPPRETTET, fnr, AdressebeskyttelseGradering.FORTROLIG)

        coEvery { grunnlagKlient.hentAlleSakIder(any()) } returns sakIder
        every { sakService.oppdaterAdressebeskyttelse(any(), any()) } returns 1
        every { sakService.finnSaker(fnr) } returns saker
        every { oppgaveService.oppdaterEnhetForRelaterteOppgaver(any()) } returns Unit
        every {
            brukerService.finnEnhetForPersonOgTema(any(), any(), any())
        } returns ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr)
        every { sakService.oppdaterEnhetForSaker(any()) } just runs
        runBlocking {
            grunnlagsendringshendelseService.oppdaterAdressebeskyttelseHendelse(adressebeskyttelse)
        }

        coVerify(exactly = 1) { grunnlagKlient.hentAlleSakIder(adressebeskyttelse.fnr) }

        sakIder.forEach {
            verify(exactly = 1) {
                sakService.oppdaterAdressebeskyttelse(
                    it,
                    adressebeskyttelse.adressebeskyttelseGradering,
                )
            }
            verify(exactly = 1) {
                oppgaveService.oppdaterEnhetForRelaterteOppgaver(any())
            }
        }
    }

    @Test
    fun `skal kunne opprette hendelser som følge av feilet regulering`() {
        val sakId = 1L
        val lagretHendelse =
            grunnlagsendringshendelseMedSamsvar(
                type = GrunnlagsendringsType.GRUNNBELOEP,
                id = randomUUID(),
                sakId = sakId,
                gjelderPerson = KONTANT_FOT.value,
                samsvarMellomKildeOgGrunnlag = null,
            )

        val hendelseSomLagres = slot<Grunnlagsendringshendelse>()

        every { sakService.finnSak(sakId) } returns
            Sak(
                KONTANT_FOT.value,
                SakType.BARNEPENSJON,
                sakId,
                Enheter.defaultEnhet.enhetNr,
            )

        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(hendelseSomLagres))
        } returns lagretHendelse
        every {
            grunnlagshendelsesDao.hentGrunnlagsendringshendelserMedStatuserISak(sakId, any())
        } returns emptyList()
        every {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(any(), any(), any(), any(), any())
        } returns mockOppgave
        every { pdlService.hentPdlModellFlereSaktyper(any(), any(), SakType.BARNEPENSJON) } returns mockPerson()
        every { behandlingService.hentBehandlingerForSak(any()) } returns emptyList()

        coEvery { grunnlagKlient.hentGrunnlag(sakId) } returns Grunnlag.empty()

        grunnlagsendringshendelseService.opprettEndretGrunnbeloepHendelse(sakId)

        assertEquals(hendelseSomLagres.captured.type, GrunnlagsendringsType.GRUNNBELOEP)
        assertEquals(hendelseSomLagres.captured.sakId, sakId)
    }
}
