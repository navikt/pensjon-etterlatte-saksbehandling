package no.nav.etterlatte.grunnlagsendring

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlagsendring.klienter.PdlKlientImpl
import no.nav.etterlatte.grunnlagsendring.klienter.hentDoedsdato
import no.nav.etterlatte.grunnlagsendringshendelseMedSamsvar
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.sak.SakServiceAdressebeskyttelse
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class GrunnlagsendringshendelseServiceTest {

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                mockk(),
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
    fun `skal opprette grunnlagsendringshendelser i databasen for doedshendelser`() {
        val sakId = 1L
        val fnr = "Soeker"
        val foerstegangsbehandlinger = listOf(
            foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.IVERKSATT),
            foerstegangsbehandling(sakId = sakId, status = BehandlingStatus.FATTET_VEDTAK)
        )
        val grunnlagsendringshendelse = grunnlagsendringshendelseMedSamsvar(
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()

        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every { oppdaterGrunnlagsendringStatus(any(), any(), any(), any()) } returns Unit
            every {
                opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
            } returns grunnlagsendringshendelse
            every { hentGrunnlagsendringshendelserMedStatuserISak(any(), any()) } returns emptyList()
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentBehandlingerISak(1L) } returns foerstegangsbehandlinger
            every { alleBehandlingerForSoekerMedFnr(fnr) } returns foerstegangsbehandlinger
            every { alleSakIderForSoekerMedFnr(fnr) } returns listOf(1L)
            every { hentSakerOgRollerMedFnrIPersongalleri(any()) } returns listOf(Pair(Saksrolle.SOEKER, sakId))
        }
        val pdlService = mockk<PdlKlientImpl>()
        val grunnlagClient = mockk<GrunnlagKlient>()
        val sakServiceAdressebeskyttelse = mockk<SakServiceAdressebeskyttelse>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService,
            grunnlagClient,
            sakServiceAdressebeskyttelse
        )

        val lagredeGrunnlagsendringshendelser = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET
            )
        )
        assertAll(
            "oppretter grunnlagshendringer i databasen for doedshendelser",
            { assertEquals(1, lagredeGrunnlagsendringshendelser.size) },
            { assertEquals(sakId, opprettGrunnlagsendringshendelse.captured.sakId) },
            { assertEquals(GrunnlagsendringsType.DOEDSFALL, opprettGrunnlagsendringshendelse.captured.type) },
            { assertTrue(opprettGrunnlagsendringshendelse.captured.opprettet >= LocalDateTime.now().minusSeconds(10)) },
            { assertEquals(1, lagredeGrunnlagsendringshendelser.size) },
            { assertEquals(grunnlagsendringshendelse, lagredeGrunnlagsendringshendelser.first()) }
        )
    }

    @Test
    fun `skal opprette grunnlagsendringshendelser i databasen for utflytting og forelder-barn`() {
        val sakId = 1L
        val fnr = "Soeker"
        val grlagEndringUtflytting = grunnlagsendringshendelseMedSamsvar(
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )
        val grlagEndringForelderBarn = grunnlagsendringshendelseMedSamsvar(
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val opprettGrlaghendelseUtflytting = slot<Grunnlagsendringshendelse>()

        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                opprettGrunnlagsendringshendelse(capture(opprettGrlaghendelseUtflytting))
            } returns grlagEndringUtflytting
            every { hentGrunnlagsendringshendelserMedStatuserISak(any(), any()) } returns emptyList()
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { alleSakIderForSoekerMedFnr(fnr) } returns listOf(1L)
            every { hentSakerOgRollerMedFnrIPersongalleri(any()) } returns listOf(Pair(Saksrolle.SOEKER, sakId))
        }
        val grunnlagClient = mockk<GrunnlagKlient>()
        val pdlService = mockk<PdlKlientImpl>()
        val sakServiceAdressebeskyttelse = mockk<SakServiceAdressebeskyttelse>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService,
            grunnlagClient,
            sakServiceAdressebeskyttelse
        )

        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            utflyttingsHendelse = UtflyttingsHendelse(
                fnr = fnr,
                endringstype = Endringstype.OPPRETTET,
                tilflyttingsLand = null,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = null
            )
        )

        val opprettGrlaghendelseForelderBarnRelasjon = slot<Grunnlagsendringshendelse>()
        every {
            grunnlagshendelsesDao.opprettGrunnlagsendringshendelse(capture(opprettGrlaghendelseForelderBarnRelasjon))
        } returns grlagEndringForelderBarn

        grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            forelderBarnRelasjonHendelse = ForelderBarnRelasjonHendelse(
                fnr = fnr,
                relatertPersonsIdent = null,
                relatertPersonsRolle = "",
                minRolleForPerson = "",
                relatertPersonUtenFolkeregisteridentifikator = null,
                endringstype = Endringstype.OPPRETTET
            )
        )

        assertEquals(
            opprettGrlaghendelseForelderBarnRelasjon.captured.type,
            GrunnlagsendringsType.FORELDER_BARN_RELASJON
        )
        assertEquals(opprettGrlaghendelseUtflytting.captured.type, GrunnlagsendringsType.UTFLYTTING)
    }

    @Test
    fun `skal ikke opprette ny doedshendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val fnr = "Soeker"
        val doedsdato = LocalDate.of(2022, 7, 8)
        val grunnlagsendringshendelse1 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.UTFLYTTING,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                opprettGrunnlagsendringshendelse(any())
            } returns grunnlagsendringshendelse1
            every {
                hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
            } returns emptyList() andThen listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2
            )
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentSakerOgRollerMedFnrIPersongalleri(any()) } returns listOf(Pair(Saksrolle.SOEKER, sakId))
        }
        val pdlService = mockk<PdlKlientImpl>()
        val grunnlagClient = mockk<GrunnlagKlient>()
        val sakServiceAdressebeskyttelse = mockk<SakServiceAdressebeskyttelse>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService,
            grunnlagClient,
            sakServiceAdressebeskyttelse
        )
        val lagredeGrunnlagsendringshendelser1 = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )
        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                fnr = fnr,
                tilflyttingsLand = null,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = null,
                endringstype = Endringstype.OPPRETTET

            )
        )

        // denne skal ikke opprette en doedshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = doedsdato,
                endringstype = Endringstype.ANNULLERT
            )
        )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal ikke opprette ny utflyttingshendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val fnr = "Soeker"
        val tilflyttingsland = "Sverige"
        val utflyttingsdato = LocalDate.of(2022, 2, 8)
        val grunnlagsendringshendelse1 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.UTFLYTTING,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )
        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = fnr,
            samsvarMellomPdlOgGrunnlag = null
        )

        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                opprettGrunnlagsendringshendelse(any())
            } returns grunnlagsendringshendelse1
            every {
                hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
            } returns emptyList() andThen listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2
            )
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentSakerOgRollerMedFnrIPersongalleri(any()) } returns listOf(Pair(Saksrolle.SOEKER, sakId))
        }
        val pdlService = mockk<PdlKlientImpl>()
        val grunnlagClient = mockk<GrunnlagKlient>()
        val sakServiceAdressebeskyttelse = mockk<SakServiceAdressebeskyttelse>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService,
            grunnlagClient,
            sakServiceAdressebeskyttelse
        )
        val lagredeGrunnlagsendringshendelser1 = grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                fnr = fnr,
                tilflyttingsLand = tilflyttingsland,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )

        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = fnr,
                doedsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            UtflyttingsHendelse(
                fnr = fnr,
                tilflyttingsLand = tilflyttingsland,
                tilflyttingsstedIUtlandet = null,
                utflyttingsdato = utflyttingsdato,
                endringstype = Endringstype.OPPRETTET
            )
        )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal ikke opprette ny forelder-barn-relasjon-hendelse dersom en lignende allerede eksisterer`() {
        val sakId = 1L
        val grunnlagsendringshendelse1 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.FORELDER_BARN_RELASJON,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = "Soeker",
            samsvarMellomPdlOgGrunnlag = null
        )
        val grunnlagsendringshendelse2 = grunnlagsendringshendelseMedSamsvar(
            type = GrunnlagsendringsType.DOEDSFALL,
            id = UUID.randomUUID(),
            sakId = sakId,
            fnr = "Soeker",
            samsvarMellomPdlOgGrunnlag = null
        )

        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                opprettGrunnlagsendringshendelse(any())
            } returns grunnlagsendringshendelse1
            every {
                hentGrunnlagsendringshendelserMedStatuserISak(any(), any())
            } returns emptyList() andThen listOf(
                grunnlagsendringshendelse1,
                grunnlagsendringshendelse2
            )
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentSakerOgRollerMedFnrIPersongalleri(any()) } returns listOf(Pair(Saksrolle.SOEKER, sakId))
        }
        val pdlService = mockk<PdlKlientImpl>()
        val grunnlagClient = mockk<GrunnlagKlient>()
        val sakServiceAdressebeskyttelse = mockk<SakServiceAdressebeskyttelse>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService,
            grunnlagClient,
            sakServiceAdressebeskyttelse
        )
        val lagredeGrunnlagsendringshendelser1 = grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            ForelderBarnRelasjonHendelse(
                fnr = "Soeker",
                relatertPersonsIdent = "Ny forelder",
                relatertPersonsRolle = null,
                minRolleForPerson = null,
                relatertPersonUtenFolkeregisteridentifikator = null,
                endringstype = Endringstype.OPPRETTET
            )
        )

        // siden dette er en hendelse av en annen type skal det ikke påvirke filtreringen
        grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = "Soeker",
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET
            )
        )

        // denne skal ikke opprette en utflyttingshendelse, siden den allerede eksisterer
        val lagredeGrunnlagsendringshendelser3 = grunnlagsendringshendelseService.opprettForelderBarnRelasjonHendelse(
            ForelderBarnRelasjonHendelse(
                fnr = "Soeker",
                relatertPersonsIdent = "Ny forelder",
                relatertPersonsRolle = null,
                minRolleForPerson = null,
                relatertPersonUtenFolkeregisteridentifikator = null,
                endringstype = Endringstype.OPPRETTET
            )
        )

        assertEquals(listOf(grunnlagsendringshendelse1), lagredeGrunnlagsendringshendelser1)
        assertEquals(emptyList<Grunnlagsendringshendelse>(), lagredeGrunnlagsendringshendelser3)
    }

    @Test
    fun `skal sette status til SJEKKET_AV_JOBB, for hendelser som er sjekket av jobb`() {
        val minutter = 60L
        val avdoedFnr = "16017919184"
        val sakId = 1L
        val grlg_id = UUID.randomUUID()
        val doedsdato = LocalDate.of(2022, 3, 13)
        val rolle = Saksrolle.SOEKER
        val personRolle = rolle.toPersonrolle()
        val grunnlagsendringshendelser = listOf(
            grunnlagsendringshendelseMedSamsvar(
                id = grlg_id,
                sakId = sakId,
                opprettet = LocalDateTime.now().minusHours(1),
                fnr = avdoedFnr,
                hendelseGjelderRolle = rolle,
                samsvarMellomPdlOgGrunnlag = null
            )
        )

        val idArg = slot<UUID>()
        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                    minutter
                )
            } returns grunnlagsendringshendelser
            every {
                oppdaterGrunnlagsendringStatus(
                    capture(idArg),
                    GrunnlagsendringStatus.VENTER_PAA_JOBB,
                    GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                    any()
                )
            } returns Unit
        }
        val mockPdlModel = mockk<PersonDTO>()
        val pdlService = mockk<PdlKlientImpl> {
            every { hentPdlModell(avdoedFnr, personRolle) } returns mockPdlModel
        }
        every { mockPdlModel.hentDoedsdato() } returns doedsdato

        val behandlingId = UUID.randomUUID()
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentBehandlingerISak(sakId) } returns listOf(
                mockk {
                    every { status } returns BehandlingStatus.VILKAARSVURDERT
                    every { id } returns behandlingId
                    every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                }
            )
        }

        val grunnlagClient = mockk<GrunnlagKlient> {
            coEvery { hentGrunnlag(any()) } returns null
        }
        val sakServiceAdressebeskyttelse = mockk<SakServiceAdressebeskyttelse>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService,
            grunnlagClient,
            sakServiceAdressebeskyttelse
        )
        grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(minutter)

        assertEquals(grlg_id, idArg.captured)
    }
}