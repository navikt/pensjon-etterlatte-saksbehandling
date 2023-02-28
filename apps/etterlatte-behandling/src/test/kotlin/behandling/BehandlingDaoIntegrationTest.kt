package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.persongalleri
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.saksbehandlerToken
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingRepo: BehandlingDao

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).apply { migrate() }

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
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        val persongalleri = persongalleri()

        val opprettBehandlingMedPersongalleri = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak1,
            persongalleri = persongalleri
        )

        behandlingRepo.opprettBehandling(opprettBehandlingMedPersongalleri)
        val opprettetBehandling = requireNotNull(
            behandlingRepo.hentBehandling(
                opprettBehandlingMedPersongalleri.id,
                BehandlingType.FØRSTEGANGSBEHANDLING
            )
        ) as Foerstegangsbehandling
        assertEquals(opprettBehandlingMedPersongalleri.id, opprettetBehandling.id)
        assertEquals(
            persongalleri.avdoed,
            opprettetBehandling.persongalleri.avdoed
        )
        assertEquals(
            persongalleri.soesken,
            opprettetBehandling.persongalleri.soesken
        )
        assertErInnenforEttSekund(
            opprettBehandlingMedPersongalleri.opprettet.toTidspunkt(),
            opprettetBehandling.behandlingOpprettet.toTidspunkt()
        )
    }

    @Test
    fun `skal opprette revurdering`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            persongalleri = persongalleri(),
            revurderingAarsak = RevurderingAarsak.SOEKER_DOD
        )

        behandlingRepo.opprettBehandling(opprettBehandling)
        val opprettetBehandling = requireNotNull(
            behandlingRepo.hentBehandling(
                opprettBehandling.id,
                BehandlingType.REVURDERING
            )
        ) as Revurdering
        assertEquals(opprettBehandling.id, opprettetBehandling.id)
        assertEquals(
            opprettBehandling.persongalleri.avdoed,
            opprettetBehandling.persongalleri.avdoed
        )
        assertEquals(
            opprettBehandling.persongalleri.soesken,
            opprettetBehandling.persongalleri.soesken
        )
        assertErInnenforEttSekund(
            opprettBehandling.opprettet.toTidspunkt(),
            opprettetBehandling.behandlingOpprettet.toTidspunkt()
        )
    }

    private fun assertErInnenforEttSekund(tidspunkt1: Tidspunkt, tidspunkt2: Tidspunkt) {
        if (tidspunkt1 != tidspunkt2) {
            LoggerFactory.getLogger(this::class.java)
                .error("Tidspunkt som skulle vært like er forskjellige: $tidspunkt1, $tidspunkt2")
        }
        assertEquals(tidspunkt1, tidspunkt2, "Tidspunkt1: $tidspunkt1, tidspunkt2: $tidspunkt2")
    }

    @Test
    fun `skal opprette manuelt opphoer`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val virkDato = YearMonth.of(2022, 8)

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.MANUELT_OPPHOER,
            sakId = sak1,
            persongalleri = persongalleri(),
            virkningstidspunkt = Virkningstidspunkt(
                virkDato,
                Grunnlagsopplysning.Saksbehandler.create(
                    saksbehandlerToken
                ),
                "begrunnelse"
            ),
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.SOESKEN_DOED,
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED
            ),
            fritekstAarsak = "Umulig å revurdere i nytt saksbehandlingssystem"
        )

        behandlingRepo.opprettBehandling(opprettBehandling)
        val opprettetBehandling = requireNotNull(
            behandlingRepo.hentBehandling(
                opprettBehandling.id,
                BehandlingType.MANUELT_OPPHOER
            )
        ) as ManueltOpphoer

        assertEquals(sak1, opprettetBehandling.sak)
        assertEquals(persongalleri(), opprettetBehandling.persongalleri)
        assertTrue(ManueltOpphoerAarsak.SOESKEN_DOED in opprettetBehandling.opphoerAarsaker)
        assertTrue(ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED in opprettetBehandling.opphoerAarsaker)
        assertEquals(virkDato, opprettetBehandling.virkningstidspunkt?.dato)
        assertEquals("Umulig å revurdere i nytt saksbehandlingssystem", opprettetBehandling.fritekstAarsak)
    }

    @Test
    fun `Skal legge til gyldighetsproeving til en opprettet behandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak1
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val lagretPersongalleriBehandling = requireNotNull(
            behandlingRepo.hentBehandling(
                opprettBehandling.id,
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
            status = BehandlingStatus.OPPRETTET
        )

        behandlingRepo.lagreGyldighetsproving(gyldighetsproevingBehanding)
        val lagretGyldighetsproving = requireNotNull(
            behandlingRepo.hentBehandling(
                opprettBehandling.id,
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
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val sak2 = sakRepo.opprettSak("321", SakType.BARNEPENSJON).id

        listOf(sak1, sak1, sak2).forEach { sak ->
            behandlingRepo.opprettBehandling(
                opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak)
            )
        }

        assertEquals(2, behandlingRepo.alleBehandlingerISak(sak1).size)
        behandlingRepo.slettBehandlingerISak(sak1)
        assertEquals(0, behandlingRepo.alleBehandlingerISak(sak1).size)
        assertEquals(1, behandlingRepo.alleBehandlingerISak(sak2).size)
    }

    @Test
    fun `avbryte sak`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        listOf(
            opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak1)
        ).forEach { b ->
            behandlingRepo.opprettBehandling(b)
        }

        var behandling = behandlingRepo.alleBehandlingerISak(sak1)
        assertEquals(1, behandling.size)
        assertEquals(false, behandling.first().status == BehandlingStatus.AVBRUTT)

        val avbruttbehandling = (behandling.first() as Foerstegangsbehandling).copy(status = BehandlingStatus.AVBRUTT)
        behandlingRepo.lagreStatus(avbruttbehandling)
        behandling = behandlingRepo.alleBehandlingerISak(sak1)
        assertEquals(1, behandling.size)
        assertEquals(true, behandling.first().status == BehandlingStatus.AVBRUTT)
    }

    @Test
    fun `skal returnere behandlingtype Foerstegangsbehandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak1).also {
            behandlingRepo.opprettBehandling(it)
        }
        val type = behandlingRepo.hentBehandlingType(opprettBehandling.id)
        assertEquals(BehandlingType.FØRSTEGANGSBEHANDLING, type)
    }

    @Test
    fun `skal returnere behandlingtype Revurdering`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            revurderingAarsak = RevurderingAarsak.SOEKER_DOD
        ).also {
            behandlingRepo.opprettBehandling(it)
        }
        val type = behandlingRepo.hentBehandlingType(opprettBehandling.id)
        assertEquals(BehandlingType.REVURDERING, type)
    }

    @Test
    fun `skal returnere behandlingtype ManueltOpphoer`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.MANUELT_OPPHOER,
            sakId = sak1,
            persongalleri = persongalleri(),
            virkningstidspunkt = Virkningstidspunkt(
                YearMonth.of(2022, 8),
                Grunnlagsopplysning.Saksbehandler.create(
                    saksbehandlerToken
                ),
                "begrunnelse"
            ),
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.SOESKEN_DOED,
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED
            ),
            fritekstAarsak = "Umulig å revurdere i nytt saksbehandlingssystem"
        ).also {
            behandlingRepo.opprettBehandling(it)
        }
        val type = behandlingRepo.hentBehandlingType(opprettBehandling.id)
        assertEquals(BehandlingType.MANUELT_OPPHOER, type)
    }

    @Test
    fun `skal hente behandling av type Foerstegangsbehandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak1).also {
            behandlingRepo.opprettBehandling(it)
        }

        val behandling = behandlingRepo.hentBehandling(
            id = opprettBehandling.id,
            type = BehandlingType.FØRSTEGANGSBEHANDLING
        )
        assertTrue(behandling is Foerstegangsbehandling)
    }

    @Test
    fun `skal returnere behandling av type Revurdering`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            revurderingAarsak = RevurderingAarsak.SOEKER_DOD
        ).also {
            behandlingRepo.opprettBehandling(it)
        }

        val behandling = behandlingRepo.hentBehandling(id = opprettBehandling.id, type = BehandlingType.REVURDERING)
        assertTrue(behandling is Revurdering)
    }

    @Test
    fun `skal returnere behandling av type ManueltOpphoer`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.MANUELT_OPPHOER,
            sakId = sak1,
            opphoerAarsaker = listOf(
                ManueltOpphoerAarsak.SOESKEN_DOED,
                ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED
            )
        ).also {
            behandlingRepo.opprettBehandling(it)
        }
        val behandling = behandlingRepo.hentBehandling(opprettBehandling.id, BehandlingType.MANUELT_OPPHOER)
        assertTrue(behandling is ManueltOpphoer)
    }

    @Test
    fun `skal returnere liste med behandlinger av ulike typer`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        repeat(2) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.REVURDERING,
                    sakId = sak1,
                    revurderingAarsak = RevurderingAarsak.SOEKER_DOD
                )
            )
        }
        repeat(2) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    sakId = sak1
                )
            )
        }

        val behandlinger = behandlingRepo.alleBehandlinger()
        assertAll(
            "Skal hente ut to foerstegangsbehandlinger og to revurderinger",
            { assertEquals(4, behandlinger.size) },
            { assertEquals(2, behandlinger.filterIsInstance<Revurdering>().size) },
            { assertEquals(2, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) }
        )
    }

    @Test
    fun `Skal bare hente behandlinger av en gitt type`() {
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON).id

        repeat(2) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.REVURDERING,
                    sakId = sak1,
                    revurderingAarsak = RevurderingAarsak.SOEKER_DOD
                )
            )
        }
        repeat(2) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    sakId = sak1
                )
            )
        }

        val foerstegangsbehandlinger = behandlingRepo.alleBehandlingerAvType(BehandlingType.FØRSTEGANGSBEHANDLING)
        val revurderinger = behandlingRepo.alleBehandlingerAvType(BehandlingType.REVURDERING)
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
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak1).also {
            behandlingRepo.opprettBehandling(it)
        }

        val behandlingFoerStatusendring =
            behandlingRepo.hentBehandling(
                opprettBehandling.id,
                BehandlingType.FØRSTEGANGSBEHANDLING
            ) as? Foerstegangsbehandling
        assertNotNull(behandlingFoerStatusendring)

        val endretTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val behandlingMedNyStatus =
            behandlingFoerStatusendring!!.copy(status = BehandlingStatus.VILKAARSVURDERT, sistEndret = endretTidspunkt)
        behandlingRepo.lagreStatus(behandlingMedNyStatus)

        val behandlingEtterStatusendring =
            behandlingRepo.hentBehandling(opprettBehandling.id, BehandlingType.FØRSTEGANGSBEHANDLING)

        assertEquals(BehandlingStatus.OPPRETTET, behandlingFoerStatusendring.status)
        assertEquals(BehandlingStatus.VILKAARSVURDERT, behandlingEtterStatusendring!!.status)
        assertEquals(endretTidspunkt, behandlingEtterStatusendring.sistEndret)
    }

    @Test
    fun `alleSakIderMedUavbruttBehandlingForSoekerMedFnr henter flere saker hvis bruker har flere`() {
        val brukerFnr = "123"
        val annenBrukerFnr = "456"
        val sakBarnepensjon = sakRepo.opprettSak(brukerFnr, SakType.BARNEPENSJON)
        val sakOmstillingstoenad = sakRepo.opprettSak(brukerFnr, SakType.OMSTILLINGSSTOENAD)

        val sakBarnepensjonAnnenBruker = sakRepo.opprettSak(annenBrukerFnr, SakType.BARNEPENSJON)
        behandlingRepo.opprettBehandling(
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakBarnepensjonAnnenBruker.id,
                persongalleri = persongalleri(soeker = annenBrukerFnr)
            )
        )

        behandlingRepo.opprettBehandling(
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakBarnepensjon.id,
                persongalleri = persongalleri(soeker = brukerFnr)
            )
        )

        behandlingRepo.opprettBehandling(
            opprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sakBarnepensjon.id,
                persongalleri = persongalleri(soeker = brukerFnr),
                revurderingAarsak = RevurderingAarsak.SOEKER_DOD
            )
        )

        behandlingRepo.opprettBehandling(
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakOmstillingstoenad.id,
                persongalleri = persongalleri(soeker = brukerFnr)
            )
        )

        assertEquals(
            behandlingRepo.alleSakIderMedUavbruttBehandlingForSoekerMedFnr(brukerFnr).toSet(),
            setOf(sakBarnepensjon.id, sakOmstillingstoenad.id)
        )
    }

    @Test
    fun `alleSakIderMedUavbruttBehandlingForSoekerMedFnr ignorerer saker med ingen uavbrutte behandlinger`() {
        val brukerFnr = "123"
        val sakBarnepensjon = sakRepo.opprettSak(brukerFnr, SakType.BARNEPENSJON)
        sakRepo.opprettSak(brukerFnr, SakType.OMSTILLINGSSTOENAD)

        behandlingRepo.opprettBehandling(
            opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sakBarnepensjon.id,
                status = BehandlingStatus.AVBRUTT,
                persongalleri = persongalleri(soeker = brukerFnr)
            )
        )
        assertEquals(
            behandlingRepo.alleSakIderMedUavbruttBehandlingForSoekerMedFnr(brukerFnr).toSet(),
            emptySet<Long>()
        )
    }

    @Test
    fun `hent alle behandlinger av type for sak henter tilbake spesifiserte saker`() {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        repeat(4) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.REVURDERING,
                    sakId = sak,
                    revurderingAarsak = RevurderingAarsak.SOEKER_DOD
                )
            )
        }

        repeat(3) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    sakId = sak
                )
            )
        }

        behandlingRepo.opprettBehandling(
            opprettBehandling(
                type = BehandlingType.MANUELT_OPPHOER,
                sakId = sak,
                opphoerAarsaker = listOf(
                    ManueltOpphoerAarsak.SOESKEN_DOED,
                    ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED
                )
            )
        )

        assertEquals(
            behandlingRepo.alleBehandlingerISakAvType(sak, BehandlingType.REVURDERING).size,
            4
        )
        assertEquals(
            behandlingRepo.alleBehandlingerISakAvType(sak, BehandlingType.FØRSTEGANGSBEHANDLING).size,
            3
        )
        assertEquals(
            behandlingRepo.alleBehandlingerISakAvType(sak, BehandlingType.MANUELT_OPPHOER).size,
            1
        )
    }

    @Test
    fun `skal hente alle loepende behandlinger i en sak`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id

        listOf(
            BehandlingStatus.OPPRETTET,
            BehandlingStatus.RETURNERT,
            BehandlingStatus.IVERKSATT,
            BehandlingStatus.AVBRUTT
        ).forEach {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    sakId = sak1,
                    status = it
                )
            )
        }

        val lagredeBehandlinger = behandlingRepo.alleBehandlingerISak(sak1)
        val alleLoependeBehandlinger = behandlingRepo.alleAktiveBehandlingerISak(sak1)
        assertEquals(4, lagredeBehandlinger.size)
        assertEquals(2, alleLoependeBehandlinger.size)
    }

    @Test
    fun `skal lagre virkningstidspunkt for en behandling`() {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak,
            status = BehandlingStatus.OPPRETTET
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val behandling = behandlingRepo.hentBehandling(opprettBehandling.id) as? Foerstegangsbehandling

        assertNotNull(behandling)

        val saksbehandler = Grunnlagsopplysning.Saksbehandler("navIdent", Instant.now())
        val nyDato = YearMonth.of(2021, 2)
        behandling!!.oppdaterVirkningstidspunkt(nyDato, saksbehandler, "enBegrunnelse").let {
            behandlingRepo.lagreNyttVirkningstidspunkt(behandling.id, it.virkningstidspunkt!!)
        }

        val expected = Virkningstidspunkt(nyDato, saksbehandler, "enBegrunnelse")
        with(behandlingRepo.hentBehandling(behandling.id)) {
            val actual = (this as Foerstegangsbehandling).virkningstidspunkt
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `skal lagre kommer barnet til gode for en behandling`() {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak,
            status = BehandlingStatus.OPPRETTET
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val kommerBarnetTilgode = KommerBarnetTilgode(
            JaNeiVetIkke.JA,
            "begrunnelse",
            Grunnlagsopplysning.Saksbehandler("navIdent", Instant.now())
        )
        behandlingRepo.lagreKommerBarnetTilgode(opprettBehandling.id, kommerBarnetTilgode)

        with(behandlingRepo.hentBehandling(opprettBehandling.id)) {
            val expected = kommerBarnetTilgode
            val actual = (this as Foerstegangsbehandling).kommerBarnetTilgode

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `skal hente ut saksnr og rolle for saker hvor et fnr opptrer`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val sak2 = sakRepo.opprettSak("321", SakType.BARNEPENSJON).id

        listOf(
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak1,
                persongalleri = Persongalleri(
                    soeker = "11111",
                    innsender = "22222",
                    soesken = listOf("33333", "44444"),
                    avdoed = listOf("55555"),
                    gjenlevende = listOf("66666")
                )
            ),
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak1,
                persongalleri = Persongalleri(
                    soeker = "77777",
                    innsender = "88888",
                    soesken = listOf("99999"),
                    avdoed = listOf("00000"),
                    gjenlevende = listOf("01010")
                )
            ),
            opprettBehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                sakId = sak2,
                persongalleri = Persongalleri(
                    soeker = "11111",
                    innsender = "11111",
                    soesken = listOf("11111", "04040", "05050"),
                    avdoed = listOf("06060", "11111"),
                    gjenlevende = listOf("11111")
                )
            )
        ).forEach {
            behandlingRepo.opprettBehandling(it)
        }

        val sakerOgRoller = behandlingRepo.sakerOgRollerMedFnrIPersongalleri("11111")
        assertEquals(5, sakerOgRoller.size)
        assertEquals(Saksrolle.SOEKER, sakerOgRoller[0].first)
        assertEquals(sak1, sakerOgRoller[0].second)
        assertEquals(Saksrolle.AVDOED, sakerOgRoller[1].first)
        assertEquals(sak2, sakerOgRoller[1].second)
        assertEquals(Saksrolle.GJENLEVENDE, sakerOgRoller[2].first)
        assertEquals(sak2, sakerOgRoller[2].second)
        assertEquals(Saksrolle.SOEKER, sakerOgRoller[3].first)
        assertEquals(sak2, sakerOgRoller[3].second)
        assertEquals(Saksrolle.SOESKEN, sakerOgRoller[4].first)
        assertEquals(sak2, sakerOgRoller[4].second)
    }

    @Test
    fun `kan oppdatere og lagre ny status for behandling`() {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak,
            persongalleri = Persongalleri(
                soeker = "11111",
                innsender = "22222",
                soesken = listOf("33333", "44444"),
                avdoed = listOf("55555"),
                gjenlevende = listOf("66666")
            )
        )
        behandlingRepo.opprettBehandling(opprettBehandling)

        val foerstegangsbehandling = behandlingRepo.hentBehandling(opprettBehandling.id) as? Foerstegangsbehandling

        assertNotNull(foerstegangsbehandling)

        val saksbehandler = Grunnlagsopplysning.Saksbehandler("saksbehandler01", Tidspunkt.now().instant)

        val kommerBarnetTilgode = KommerBarnetTilgode(JaNeiVetIkke.JA, "", saksbehandler)
        val virkningstidspunkt = Virkningstidspunkt(YearMonth.of(2021, 1), saksbehandler, "")
        val gyldighetsResultat = GyldighetsResultat(VurderingsResultat.OPPFYLT, listOf(), LocalDateTime.now())

        foerstegangsbehandling!!
            .oppdaterKommerBarnetTilgode(kommerBarnetTilgode)
            .oppdaterVirkningstidspunkt(virkningstidspunkt.dato, virkningstidspunkt.kilde, "")
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert(VilkaarsvurderingUtfall.OPPFYLT)
            .let {
                behandlingRepo.lagreNyttVirkningstidspunkt(it.id, it.virkningstidspunkt!!)
                behandlingRepo.lagreGyldighetsproving(it)
                behandlingRepo.lagreKommerBarnetTilgode(it.id, it.kommerBarnetTilgode!!)
                behandlingRepo.lagreStatus(it.id, it.status, it.sistEndret)
            }

        with(behandlingRepo.hentBehandling(foerstegangsbehandling.id) as Foerstegangsbehandling) {
            assertEquals(BehandlingStatus.VILKAARSVURDERT, this.status)
            assertEquals(kommerBarnetTilgode, this.kommerBarnetTilgode)
            assertEquals(virkningstidspunkt, this.virkningstidspunkt)
            assertEquals(gyldighetsResultat, this.gyldighetsproeving)
            assert(this.sistEndret > foerstegangsbehandling.sistEndret)
        }
    }
}