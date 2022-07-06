package no.nav.etterlatte.itest

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.Foerstegangsbehandling
import no.nav.etterlatte.behandling.Revurdering
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.revurdering
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")

    private lateinit var dataSource: DataSource
    lateinit var sakRepo: SakDao
    lateinit var behandlingRepo: BehandlingDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()

        val connection = dataSource.connection
        sakRepo = SakDao { connection }
        behandlingRepo = BehandlingDao { connection }

    }


    @AfterEach
    fun afterEach() {
        dataSource.connection.use {
            it.prepareStatement("TRUNCATE behandling CASCADE;").execute()
        }
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal opprette foerstegangsbehandling med persongalleri`() {
        val sak1 = sakRepo.opprettSak("123", "BP").id
        val behandlingOpprettet = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)

        val persongalleri = persongalleri()
        val behandlingMedPersongalleri =
            foerstegangsbehandling(sak = sak1, persongalleri = persongalleri, behandlingOpprettet = behandlingOpprettet)

        behandlingRepo.opprettFoerstegangsbehandling(behandlingMedPersongalleri)
        val opprettetBehandling =
            requireNotNull(
                behandlingRepo.hentBehandling(
                    behandlingMedPersongalleri.id,
                    BehandlingType.FØRSTEGANGSBEHANDLING
                )
            ) as Foerstegangsbehandling
        assertEquals(behandlingMedPersongalleri.id, opprettetBehandling.id)
        assertEquals(
            behandlingMedPersongalleri.persongalleri.avdoed,
            opprettetBehandling.persongalleri.avdoed
        )
        assertEquals(
            behandlingMedPersongalleri.persongalleri.soesken,
            opprettetBehandling.persongalleri.soesken
        )
        assertEquals(
            behandlingMedPersongalleri.behandlingOpprettet,
            opprettetBehandling.behandlingOpprettet
        )

    }


    @Test
    fun `skal opprette revurdering`() {
        val sak1 = sakRepo.opprettSak("123", "BP").id
        val behandlingOpprettet = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)

        val behandling = revurdering(sak = sak1, behandlingOpprettet = behandlingOpprettet)
        behandlingRepo.opprettRevurdering(behandling)
        val opprettetBehandling =
            requireNotNull(
                behandlingRepo.hentBehandling(
                    behandling.id,
                    BehandlingType.REVURDERING
                )
            ) as Revurdering
        assertEquals(behandling.id, opprettetBehandling.id)
        assertEquals(
            behandling.persongalleri.avdoed,
            opprettetBehandling.persongalleri.avdoed
        )
        assertEquals(
            behandling.persongalleri.soesken,
            opprettetBehandling.persongalleri.soesken
        )
        assertEquals(
            behandling.behandlingOpprettet,
            opprettetBehandling.behandlingOpprettet
        )

    }

    @Test
    fun `Skal legge til gyldighetsprøving til en opprettet behandling`() {
        val sak1 = sakRepo.opprettSak("123", "BP").id

        val behandling = foerstegangsbehandling(sak = sak1)

        behandlingRepo.opprettFoerstegangsbehandling(behandling)

        val lagretPersongalleriBehandling =
            requireNotNull(
                behandlingRepo.hentBehandling(
                    behandling.id,
                    BehandlingType.FØRSTEGANGSBEHANDLING
                )
            ) as Foerstegangsbehandling

        val gyldighetsproevingBehanding = lagretPersongalleriBehandling.copy(
            gyldighetsproeving = GyldighetsResultat(
                resultat = VurderingsResultat.OPPFYLT,
                vurderinger = listOf(
                    VurdertGyldighet(
                        navn = GyldighetsTyper.INNSENDER_ER_FORELDER,
                        resultat = VurderingsResultat.OPPFYLT,
                        basertPaaOpplysninger = "innsenderfnr"
                    )
                ),
                vurdertDato = LocalDateTime.now()
            ),
            status = BehandlingStatus.GYLDIG_SOEKNAD,
            oppgaveStatus = OppgaveStatus.NY
        )

        behandlingRepo.lagreGyldighetsproving(gyldighetsproevingBehanding)
        val lagretGyldighetsproving =
            requireNotNull(
                behandlingRepo.hentBehandling(
                    behandling.id,
                    BehandlingType.FØRSTEGANGSBEHANDLING
                )
            ) as Foerstegangsbehandling

        assertEquals(
            gyldighetsproevingBehanding.gyldighetsproeving,
            lagretGyldighetsproving.gyldighetsproeving
        )

    }

    @Test
    fun `Sletting av alle behandlinger i en sak`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id
        val sak2 = sakRepo.opprettSak("321", "BP").id

        listOf(
            foerstegangsbehandling(sak = sak1),
            foerstegangsbehandling(sak = sak1),
            foerstegangsbehandling(sak = sak2)
        ).forEach { b ->
            behandlingRepo.opprettFoerstegangsbehandling(b)
        }

        assertEquals(2, behandlingRepo.alleBehandingerISak(sak1).size)
        behandlingRepo.slettBehandlingerISak(sak1)
        assertEquals(0, behandlingRepo.alleBehandingerISak(sak1).size)
        assertEquals(1, behandlingRepo.alleBehandingerISak(sak2).size)

    }


    @Test
    fun `avbryte sak`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id
        listOf(
            foerstegangsbehandling(sak = sak1)
        ).forEach { b ->
            behandlingRepo.opprettFoerstegangsbehandling(b)
        }

        var behandling = behandlingRepo.alleBehandingerISak(sak1)
        assertEquals(1, behandling.size)
        assertEquals(false, behandling.first().status == BehandlingStatus.AVBRUTT)

        val avbruttbehandling = (behandling.first() as Foerstegangsbehandling).copy(status = BehandlingStatus.AVBRUTT)
        behandlingRepo.lagreStatus(avbruttbehandling)
        behandling = behandlingRepo.alleBehandingerISak(sak1)
        assertEquals(1, behandling.size)
        assertEquals(true, behandling.first().status == BehandlingStatus.AVBRUTT)

    }

    @Test
    fun `skal returnere behandlingtype Foerstegangsbehandling`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id

        val foerstegangsbehandling = foerstegangsbehandling(sak = sak1)
            .also {
                behandlingRepo.opprettFoerstegangsbehandling(it)
            }

        val type = behandlingRepo.hentBehandlingType(foerstegangsbehandling.id)
        assertEquals(BehandlingType.FØRSTEGANGSBEHANDLING, type)


    }

    @Test
    fun `skal returnere behandlingtype Revurdering`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id

        val revurdering = revurdering(sak = sak1)
            .also {
                behandlingRepo.opprettRevurdering(it)
            }

        val type = behandlingRepo.hentBehandlingType(revurdering.id)
        assertEquals(BehandlingType.REVURDERING, type)
    }


    @Test
    fun `skal hente behandling av type Foerstegangsbehandling`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id

        val foerstegangsbehandling = foerstegangsbehandling(sak = sak1)
            .also {
                behandlingRepo.opprettFoerstegangsbehandling(it)
            }

        val behandling = behandlingRepo.hentBehandling(
            id = foerstegangsbehandling.id,
            type = BehandlingType.FØRSTEGANGSBEHANDLING
        )
        assertTrue(behandling is Foerstegangsbehandling)
    }


    @Test
    fun `skal returnere behandling av type Revurdering`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id

        val revurdering = revurdering(sak = sak1)
            .also {
                behandlingRepo.opprettRevurdering(it)
            }

        val behandling = behandlingRepo.hentBehandling(id = revurdering.id, type = BehandlingType.REVURDERING)
        assertTrue(behandling is Revurdering)
    }

    @Test
    fun `skal returnere liste med behandlinger av ulike typer`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id

        listOf(
            revurdering(sak = sak1),
            revurdering(sak = sak1)
        ).forEach {
            behandlingRepo.opprettRevurdering(it)
        }
        listOf(
            foerstegangsbehandling(sak = sak1),
            foerstegangsbehandling(sak = sak1)
        ).forEach {
            behandlingRepo.opprettFoerstegangsbehandling(it)
        }

        val behandlinger = behandlingRepo.alleBehandlinger()
        assertAll(
            "Skal hente ut to foerstegangsbehandlinger og to revurderinger",
            { assertEquals(4, behandlinger.size) },
            { assertEquals(2, behandlinger.filterIsInstance<Revurdering>().size) },
            { assertEquals(2, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) },
        )
    }

    @Test
    fun `Skal bare hente behandlinger av en gitt type`() {


        val sak1 = sakRepo.opprettSak("1234", "BP").id

        val rev = listOf(
            revurdering(sak = sak1),
            revurdering(sak = sak1)
        ).forEach {
            behandlingRepo.opprettRevurdering(it)
        }
        val foer = listOf(
            foerstegangsbehandling(sak = sak1),
            foerstegangsbehandling(sak = sak1)
        ).forEach {
            behandlingRepo.opprettFoerstegangsbehandling(it)
        }

        val foerstegangsbehandlinger = behandlingRepo.alleBehandlinger(BehandlingType.FØRSTEGANGSBEHANDLING)
        val revurderinger = behandlingRepo.alleBehandlinger(BehandlingType.REVURDERING)
        assertAll(
            "Skal hente ut to foerstegangsbehandlinger og to revurderinger",
            { assertEquals(2, foerstegangsbehandlinger.size) },
            { assertTrue(foerstegangsbehandlinger.all { it is Foerstegangsbehandling }) },
            { assertEquals(2, revurderinger.size) },
            {
                assertTrue(revurderinger.all { it is Revurdering })
            }
        )
    }

    @Test
    fun `skal lagre status og sette sistEndret for en behandling`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id

        val behandling = foerstegangsbehandling(sak = sak1)
            .also {
                behandlingRepo.opprettFoerstegangsbehandling(it)
            }

        val behandlingFoerStatusendring =
            behandlingRepo.hentBehandling(behandling.id, BehandlingType.FØRSTEGANGSBEHANDLING)
        val endretTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val behandlingMedNyStatus =
            behandling.copy(status = BehandlingStatus.UNDER_BEHANDLING, sistEndret = endretTidspunkt)
        behandlingRepo.lagreStatus(behandlingMedNyStatus)
        val behandlingEtterStatusendring =
            behandlingRepo.hentBehandling(behandling.id, BehandlingType.FØRSTEGANGSBEHANDLING)


        assertEquals(BehandlingStatus.OPPRETTET, behandlingFoerStatusendring!!.status)
        assertEquals(BehandlingStatus.UNDER_BEHANDLING, behandlingEtterStatusendring!!.status)
        assertEquals(endretTidspunkt, behandlingEtterStatusendring!!.sistEndret)
    }

    @Test
    fun `skal lagre oppgavestatus for behandling`() {

        val sak1 = sakRepo.opprettSak("123", "BP").id

        val behandling = foerstegangsbehandling(sak = sak1)
            .also {
                behandlingRepo.opprettFoerstegangsbehandling(it)
            }

        val behandlingFoerStatusendring =
            behandlingRepo.hentBehandling(behandling.id, BehandlingType.FØRSTEGANGSBEHANDLING)
        val endretTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val behandlingMedNyStatus =
            behandling.copy(oppgaveStatus = OppgaveStatus.LUKKET, sistEndret = endretTidspunkt)
        behandlingRepo.lagreOppgaveStatus(behandlingMedNyStatus)
        val behandlingEtterStatusendring =
            behandlingRepo.hentBehandling(behandling.id, BehandlingType.FØRSTEGANGSBEHANDLING)

        assertEquals(OppgaveStatus.NY, behandlingFoerStatusendring!!.oppgaveStatus)
        assertEquals(OppgaveStatus.LUKKET, behandlingEtterStatusendring!!.oppgaveStatus)
        assertEquals(endretTidspunkt, behandlingEtterStatusendring!!.sistEndret)

    }


}
