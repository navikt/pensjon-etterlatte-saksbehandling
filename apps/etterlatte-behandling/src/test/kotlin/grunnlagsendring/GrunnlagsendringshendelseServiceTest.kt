package no.nav.etterlatte.grunnlagsendring

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.Behandling
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.revurdering.RevurderingService
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendringshendelse
import no.nav.etterlatte.grunnlagsinformasjonDoedshendelse
import no.nav.etterlatte.grunnlagsinformasjonForelderBarnRelasjonHendelse
import no.nav.etterlatte.grunnlagsinformasjonUtflyttingshendelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsType
import no.nav.etterlatte.libs.common.behandling.Grunnlagsendringshendelse
import no.nav.etterlatte.libs.common.behandling.Grunnlagsinformasjon
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.PdlHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.person.Person
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
            every { oppdaterGrunnlagsendringStatusForType(any(), any(), any(), any()) } returns Unit
            every {
                opprettGrunnlagsendringshendelse(capture(opprettGrunnlagsendringshendelse))
            } returns grunnlagsendringshendelse
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { hentBehandlingerISak(1L) } returns foerstegangsbehandlinger
            every { alleBehandlingerForSoekerMedFnr("Soeker") } returns foerstegangsbehandlinger
            every { alleSakIderForSoekerMedFnr("Soeker") } returns listOf(1L)
        }
        val revurderingService = mockk<RevurderingService>()
        val pdlService = mockk<PdlService>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            revurderingService,
            pdlService
        )

        val lagredeGrunnlagsendringshendelser = grunnlagsendringshendelseService.opprettSoekerDoedHendelse(
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
            { assertEquals(GrunnlagsendringsType.SOEKER_DOED, opprettGrunnlagsendringshendelse.captured.type) },
            { assertTrue(opprettGrunnlagsendringshendelse.captured.data is Grunnlagsinformasjon.SoekerDoed) },
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
        }
        val revurderingService = mockk<RevurderingService>()
        val pdlService = mockk<PdlService>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            revurderingService,
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
    fun `sjekkKlareDoedshendelser skal oppdatere ikke-vurderte-grunnlagsendringshendelser til status forkastet paa ikke-avbrutte saker`() { // ktlint-disable max-line-length
        val sakId1 = 1L
        val sakId2 = 2L
        val sakId3 = 3L
        val foerstegangsbehandlinger = listOf(
            foerstegangsbehandling(sak = sakId1, status = BehandlingStatus.IVERKSATT),
            foerstegangsbehandling(sak = sakId2, status = BehandlingStatus.GYLDIG_SOEKNAD),
            foerstegangsbehandling(sak = sakId3, status = BehandlingStatus.AVBRUTT)
        )
        val sakerArg = slot<List<Long>>()
        val foerStatusArg = slot<GrunnlagsendringStatus>()
        val etterStatusArg = slot<GrunnlagsendringStatus>()
        val typeArg = slot<GrunnlagsendringsType>()
        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                oppdaterGrunnlagsendringStatusForType(
                    capture(sakerArg),
                    capture(foerStatusArg),
                    capture(etterStatusArg),
                    capture(typeArg)
                )
            } returns Unit
            every { opprettGrunnlagsendringshendelse(any()) } returns mockk()
        }
        val generellBehandlingService = mockk<GenerellBehandlingService> {
            every { alleBehandlingerForSoekerMedFnr("Soeker") } returns foerstegangsbehandlinger
            every { alleSakIderForSoekerMedFnr("Soeker") } returns listOf(sakId1, sakId2)
        }
        val revurderingService = mockk<RevurderingService>()
        val pdlService = mockk<PdlService>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            revurderingService,
            pdlService
        )
        grunnlagsendringshendelseService.opprettSoekerDoedHendelse(
            Doedshendelse(
                avdoedFnr = "Soeker",
                doedsdato = LocalDate.of(2022, 1, 1),
                endringstype = Endringstype.OPPRETTET
            )
        )

        assertEquals(listOf(1L, 2L), sakerArg.captured)
        assertEquals(GrunnlagsendringStatus.IKKE_VURDERT, foerStatusArg.captured)
        assertEquals(GrunnlagsendringStatus.FORKASTET, etterStatusArg.captured)
        assertEquals(GrunnlagsendringsType.SOEKER_DOED, typeArg.captured)
    }

    @Test
    fun `skal forkaste doedshendelser hvor soeker ikke er doed i pdl`() {
        val minutter = 60L
        val avdoedFnr = "soeker"
        val sakId = 1L
        val grunnlagsendringshendelser = listOf(
            grunnlagsendringshendelse(
                sakId = sakId,
                opprettet = LocalDateTime.now().minusHours(1),
                data = grunnlagsinformasjonDoedshendelse(avdoedFnr = avdoedFnr)
            )
        )
        val sakerArg = slot<List<Long>>()
        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                    minutter,
                    GrunnlagsendringsType.SOEKER_DOED
                )
            } returns grunnlagsendringshendelser
            every {
                oppdaterGrunnlagsendringStatusForType(
                    capture(sakerArg),
                    GrunnlagsendringStatus.IKKE_VURDERT,
                    GrunnlagsendringStatus.FORKASTET,
                    GrunnlagsendringsType.SOEKER_DOED
                )
            } returns Unit
        }
        val avdoedFnrArg = slot<String>()
        val pdlService = mockk<PdlService> {
            every { hentPdlModell(capture(avdoedFnrArg), PersonRolle.BARN) } returns mockk<Person>() {
                every { doedsdato } returns null
            }
        }
        val generellBehandlingService = mockk<GenerellBehandlingService>()
        val revurderingService = mockk<RevurderingService>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            revurderingService,
            pdlService
        )
        grunnlagsendringshendelseService.sjekkKlareDoedshendelser(minutter)

        assertEquals(avdoedFnr, avdoedFnrArg.captured)
        assertEquals(sakId, sakerArg.captured.first())
    }

    @Test
    fun `skal starte revurdering for sak uten aktive behandlinger`() {
        val minutter = 60L
        val avdoedFnr = "soeker"
        val sakId = 1L
        val grunnlagsendringshendelser = listOf(
            grunnlagsendringshendelse(
                sakId = sakId,
                opprettet = LocalDateTime.now().minusHours(1),
                data = grunnlagsinformasjonDoedshendelse(avdoedFnr = avdoedFnr)
            )
        )
        val sakerArg = slot<List<Long>>()
        val behandlingReferanse = slot<UUID>()
        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                    minutter,
                    GrunnlagsendringsType.SOEKER_DOED
                )
            } returns grunnlagsendringshendelser
            every {
                oppdaterGrunnlagsendringStatusForType(
                    capture(sakerArg),
                    GrunnlagsendringStatus.IKKE_VURDERT,
                    GrunnlagsendringStatus.TATT_MED_I_BEHANDLING,
                    GrunnlagsendringsType.SOEKER_DOED
                )
            } returns Unit
            every {
                settBehandlingIdForTattMedIBehandling(
                    any(),
                    capture(behandlingReferanse),
                    GrunnlagsendringsType.SOEKER_DOED
                )
            } returns Unit
        }
        val pdlService = mockk<PdlService> {
            every { hentPdlModell(avdoedFnr, PersonRolle.BARN) } returns mockk {
                every { doedsdato } returns LocalDate.of(2022, 10, 8)
            }
        }
        val behandlingId = UUID.randomUUID()
        val generellBehandlingService = mockk<GenerellBehandlingService>() {
            every { hentBehandlingerISak(sakId) } returns listOf(
                mockk<Behandling>() {
                    every { status } returns BehandlingStatus.IVERKSATT
                    every { id } returns behandlingId
                }
            )
        }
        val behandlingArg = slot<Behandling>()
        val endringshendelseArg = slot<PdlHendelse>()
        val revurderingAarsakArg = slot<RevurderingAarsak>()
        val revurderingService = mockk<RevurderingService> {
            every {
                startRevurdering(
                    capture(behandlingArg),
                    capture(endringshendelseArg),
                    capture(revurderingAarsakArg)
                )
            } returns mockk() {
                every { id } returns behandlingId
            }
        }
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            revurderingService,
            pdlService
        )
        grunnlagsendringshendelseService.sjekkKlareDoedshendelser(minutter)

        assertEquals(sakId, sakerArg.captured.first())
        assertEquals(behandlingId, behandlingArg.captured.id)
        assertTrue(endringshendelseArg.captured is Doedshendelse)
        assertEquals(RevurderingAarsak.SOEKER_DOD, revurderingAarsakArg.captured)
        assertEquals(behandlingReferanse.captured, behandlingArg.captured.id)
    }

    @Test
    fun `skal ikke opprette revurdering, men sette status til GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING, for saker med aktive behandlinger`() { // ktlint-disable max-line-length
        val minutter = 60L
        val avdoedFnr = "soeker"
        val sakId = 1L
        val grunnlagsendringshendelser = listOf(
            grunnlagsendringshendelse(
                sakId = sakId,
                opprettet = LocalDateTime.now().minusHours(1),
                data = grunnlagsinformasjonDoedshendelse(avdoedFnr = avdoedFnr)
            )
        )
        val sakerArg = slot<List<Long>>()
        val grunnlagshendelsesDao = mockk<GrunnlagsendringshendelseDao> {
            every {
                hentIkkeVurderteGrunnlagsendringshendelserEldreEnn(
                    minutter,
                    GrunnlagsendringsType.SOEKER_DOED
                )
            } returns grunnlagsendringshendelser
            every {
                oppdaterGrunnlagsendringStatusForType(
                    capture(sakerArg),
                    GrunnlagsendringStatus.IKKE_VURDERT,
                    GrunnlagsendringStatus.GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING,
                    GrunnlagsendringsType.SOEKER_DOED
                )
            } returns Unit
        }
        val pdlService = mockk<PdlService> {
            every { hentPdlModell(avdoedFnr, PersonRolle.BARN) } returns mockk {
                every { doedsdato } returns LocalDate.of(2022, 10, 8)
            }
        }
        val behandlingId = UUID.randomUUID()
        val generellBehandlingService = mockk<GenerellBehandlingService>() {
            every { hentBehandlingerISak(sakId) } returns listOf(
                mockk {
                    every { status } returns BehandlingStatus.UNDER_BEHANDLING
                    every { id } returns behandlingId
                }
            )
        }
        val revurderingService = mockk<RevurderingService>()
        val grunnlagsendringshendelseService = GrunnlagsendringshendelseService(
            grunnlagshendelsesDao,
            generellBehandlingService,
            revurderingService,
            pdlService
        )
        grunnlagsendringshendelseService.sjekkKlareDoedshendelser(minutter)

        assertEquals(sakId, sakerArg.captured.first())
        verify(exactly = 0) { revurderingService.startRevurdering(any(), any(), any()) }
    }
}