package no.nav.etterlatte.grunnlagsendring

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendringshendelse
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.grunnlagsinformasjonForelderBarnRelasjonHendelse
import no.nav.etterlatte.grunnlagsinformasjonUtflyttingshendelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon
import no.nav.etterlatte.libs.common.behandling.KorrektIPDL
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.PdlService
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
        val foerstegangsbehandlinger = listOf(
            foerstegangsbehandling(sak = sakId, status = BehandlingStatus.IVERKSATT),
            foerstegangsbehandling(sak = sakId, status = BehandlingStatus.FATTET_VEDTAK)
        )
        val grunnlagsendringshendelse =
            grunnlagsendringshendelse(
                id = UUID.randomUUID(),
                sakId = sakId,
                data = grunnlagsinformasjonDoedshendelse("Soeker")
            )

        val opprettGrunnlagsendringshendelse = slot<Grunnlagsendringshendelse>()

        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every { oppdaterGrunnlagsendringStatus(any(), any(), any(), any()) } returns Unit
            every {
                opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
            } returns grunnlagsendringshendelse
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentBehandlingerISak(1L) } returns foerstegangsbehandlinger
            every { alleBehandlingerForSoekerMedFnr("Soeker") } returns foerstegangsbehandlinger
            every { alleSakIderForSoekerMedFnr("Soeker") } returns listOf(1L)
            every { hentSakerOgRollerMedFnrIPersongalleri(any()) } returns listOf(Pair(Saksrolle.SOEKER, sakId))
        }
        val pdlService = mockk<PdlService>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService
        )

        val lagredeGrunnlagsendringshendelser = grunnlagsendringshendelseService.opprettDoedshendelse(
            Doedshendelse(
                avdoedFnr = "Soeker",
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET
            )
        )
        assertAll(
            "oppretter grunnlagshendringer i databasen for doedshendelser",
            { assertEquals(1, lagredeGrunnlagsendringshendelser.size) },
            { assertEquals(sakId, opprettGrunnlagsendringshendelse.captured.sakId) },
            { assertEquals(GrunnlagsendringsType.DOEDSFALL, opprettGrunnlagsendringshendelse.captured.type) },
            { assertTrue(opprettGrunnlagsendringshendelse.captured.data is Grunnlagsinformasjon.Doedsfall) },
            { assertTrue(opprettGrunnlagsendringshendelse.captured.opprettet >= LocalDateTime.now().minusSeconds(10)) },
            { assertEquals(1, lagredeGrunnlagsendringshendelser.size) },
            { assertEquals(grunnlagsendringshendelse.id, lagredeGrunnlagsendringshendelser.first().id) },
            { assertEquals(grunnlagsendringshendelse.data, lagredeGrunnlagsendringshendelser.first().data) },
            { assertEquals(grunnlagsendringshendelse.sakId, lagredeGrunnlagsendringshendelser.first().sakId) },
            { assertEquals(grunnlagsendringshendelse.type, lagredeGrunnlagsendringshendelser.first().type) },
            { assertEquals(grunnlagsendringshendelse.opprettet, lagredeGrunnlagsendringshendelser.first().opprettet) },
            { assertEquals(grunnlagsendringshendelse.status, lagredeGrunnlagsendringshendelser.first().status) }
        )
    }

    @Test
    fun `skal opprette grunnlagsendringshendelser i databasen for utflytting og forelder-barn`() {
        val sakId = 1L
        val grlagEndringUtflytting =
            grunnlagsendringshendelse(
                id = UUID.randomUUID(),
                sakId = sakId,
                data = grunnlagsinformasjonUtflyttingshendelse("Soeker")
            )
        val grlagEndringForelderBarn =
            grunnlagsendringshendelse(
                id = UUID.randomUUID(),
                sakId = sakId,
                data = grunnlagsinformasjonForelderBarnRelasjonHendelse("Soeker")
            )

        val opprettGrlaghendelseUtflytting = slot<Grunnlagsendringshendelse>()

        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                opprettGrunnlagsendringshendelse(capture(opprettGrlaghendelseUtflytting))
            } returns grlagEndringUtflytting
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { alleSakIderForSoekerMedFnr("Soeker") } returns listOf(1L)
            every { hentSakerOgRollerMedFnrIPersongalleri(any()) } returns listOf(Pair(Saksrolle.SOEKER, sakId))
        }
        val pdlService = mockk<PdlService>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService
        )

        grunnlagsendringshendelseService.opprettUtflyttingshendelse(
            utflyttingsHendelse = UtflyttingsHendelse(
                fnr = "Soeker",
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
                fnr = "Soeker",
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
    fun `skal sette status til SJEKKET_AV_JOBB, for hendelser som er sjekket av jobb`() {
        val minutter = 60L
        val avdoedFnr = "soeker"
        val sakId = 1L
        val grlg_id = UUID.randomUUID()
        val grunnlagsendringshendelser = listOf(
            grunnlagsendringshendelse(
                id = grlg_id,
                sakId = sakId,
                opprettet = LocalDateTime.now().minusHours(1),
                data = grunnlagsinformasjonDoedshendelse(avdoedFnr = avdoedFnr)
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
                    korrektIPDL = KorrektIPDL.JA
                )
            } returns Unit
        }
        val pdlService = mockk<PdlService> {
            every { hentPdlModell(avdoedFnr, PersonRolle.BARN) } returns mockk {
                every { doedsdato } returns LocalDate.of(2022, 10, 8)
            }
            every { personErDoed(avdoedFnr) } returns KorrektIPDL.JA
        }
        val behandlingId = UUID.randomUUID()
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentBehandlingerISak(sakId) } returns listOf(
                mockk {
                    every { status } returns BehandlingStatus.VILKAARSVURDERING
                    every { id } returns behandlingId
                    every { type } returns BehandlingType.FØRSTEGANGSBEHANDLING
                }
            )
        }
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            pdlService
        )
        grunnlagsendringshendelseService.sjekkKlareGrunnlagsendringshendelser(minutter)

        assertEquals(grlg_id, idArg.captured)
    }
}