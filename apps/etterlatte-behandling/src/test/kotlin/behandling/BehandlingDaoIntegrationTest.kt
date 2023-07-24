package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.ManueltOpphoer
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.manueltopphoer.ManueltOpphoerAarsak
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.UtenlandstilsnittType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var dataSource: DataSource
    private lateinit var sakRepo: SakDao
    private lateinit var behandlingRepo: BehandlingDao
    private lateinit var kommerBarnetTilGodeDao: KommerBarnetTilGodeDao

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
        kommerBarnetTilGodeDao = KommerBarnetTilGodeDao { connection }
        behandlingRepo = BehandlingDao(kommerBarnetTilGodeDao = kommerBarnetTilGodeDao, connection = { connection })
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
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val persongalleri = persongalleri()

        val opprettBehandlingMedPersongalleri = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak1,
            persongalleri = persongalleri
        )

        behandlingRepo.opprettBehandling(opprettBehandlingMedPersongalleri)
        val opprettetBehandling = requireNotNull(
            behandlingRepo.hentBehandling(opprettBehandlingMedPersongalleri.id)
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
        assertEquals(
            opprettBehandlingMedPersongalleri.opprettet,
            opprettetBehandling.behandlingOpprettet.toTidspunkt()
        )
    }

    @Test
    fun `skal opprette revurdering`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            persongalleri = persongalleri(),
            revurderingAarsak = RevurderingAarsak.REGULERING,
            prosesstype = Prosesstype.MANUELL
        )

        behandlingRepo.opprettBehandling(opprettBehandling)
        val opprettetBehandling = requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as Revurdering

        assertEquals(opprettBehandling.id, opprettetBehandling.id)
        assertEquals(opprettBehandling.persongalleri.avdoed, opprettetBehandling.persongalleri.avdoed)
        assertEquals(opprettBehandling.persongalleri.soesken, opprettetBehandling.persongalleri.soesken)
        assertEquals(opprettBehandling.opprettet, opprettetBehandling.behandlingOpprettet.toTidspunkt())
    }

    @Test
    fun `skal opprette manuelt opphoer`() {
        val sakId = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val virkDato = YearMonth.of(2022, 8)

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.MANUELT_OPPHOER,
            sakId = sakId,
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
        val opprettetBehandling = requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as ManueltOpphoer

        assertEquals(sakId, opprettetBehandling.sak.id)
        assertEquals(persongalleri(), opprettetBehandling.persongalleri)
        assertTrue(ManueltOpphoerAarsak.SOESKEN_DOED in opprettetBehandling.opphoerAarsaker)
        assertTrue(ManueltOpphoerAarsak.GJENLEVENDE_FORELDER_DOED in opprettetBehandling.opphoerAarsaker)
        assertEquals(virkDato, opprettetBehandling.virkningstidspunkt?.dato)
        assertEquals("Umulig å revurdere i nytt saksbehandlingssystem", opprettetBehandling.fritekstAarsak)
    }

    @Test
    fun `Skal legge til gyldighetsproeving til en opprettet behandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak1
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val lagretPersongalleriBehandling =
            requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as Foerstegangsbehandling

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
                vurdertDato = Tidspunkt.now().toLocalDatetimeUTC()
            ),
            status = BehandlingStatus.OPPRETTET
        )

        behandlingRepo.lagreGyldighetsproving(gyldighetsproevingBehanding)
        val lagretGyldighetsproving =
            requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as Foerstegangsbehandling

        assertEquals(
            gyldighetsproevingBehanding.gyldighetsproeving,
            lagretGyldighetsproving.gyldighetsproeving
        )
    }

    @Test
    fun `Skal legge til utenlandstilsnitt til en opprettet behandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak1
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val opprettetBehandling =
            requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as Foerstegangsbehandling

        behandlingRepo.lagreUtenlandstilsnitt(
            opprettetBehandling.id,
            Utenlandstilsnitt(
                UtenlandstilsnittType.BOSATT_UTLAND,
                Grunnlagsopplysning.Saksbehandler.create("navIdent"),
                "Test"
            )
        )

        val lagretBehandling =
            requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as Foerstegangsbehandling

        assertEquals(UtenlandstilsnittType.BOSATT_UTLAND, lagretBehandling.utenlandstilsnitt?.type)
        assertEquals("Test", lagretBehandling.utenlandstilsnitt?.begrunnelse)
        assertEquals("navIdent", lagretBehandling.utenlandstilsnitt?.kilde?.ident)
    }

    @Test
    fun `Skal legge til bodd eller arbeidet utlandet til en opprettet behandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak1
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val opprettetBehandling =
            requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as Foerstegangsbehandling

        behandlingRepo.lagreBoddEllerArbeidetUtlandet(
            opprettetBehandling.id,
            BoddEllerArbeidetUtlandet(
                true,
                Grunnlagsopplysning.Saksbehandler.create("navIdent"),
                "Test"
            )
        )

        val lagretBehandling =
            requireNotNull(behandlingRepo.hentBehandling(opprettBehandling.id)) as Foerstegangsbehandling

        assertEquals(true, lagretBehandling.boddEllerArbeidetUtlandet?.boddEllerArbeidetUtlandet)
        assertEquals("Test", lagretBehandling.boddEllerArbeidetUtlandet?.begrunnelse)
        assertEquals("navIdent", lagretBehandling.boddEllerArbeidetUtlandet?.kilde?.ident)
    }

    @Test
    fun `avbryte sak`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
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
    fun `skal hente behandling av type Foerstegangsbehandling`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak1).also {
            behandlingRepo.opprettBehandling(it)
        }

        val behandling = behandlingRepo.hentBehandling(opprettBehandling.id)
        assertTrue(behandling is Foerstegangsbehandling)
    }

    @Test
    fun `skal returnere behandling av type Revurdering`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val opprettBehandling = opprettBehandling(
            type = BehandlingType.REVURDERING,
            sakId = sak1,
            revurderingAarsak = RevurderingAarsak.REGULERING,
            prosesstype = Prosesstype.AUTOMATISK
        ).also {
            behandlingRepo.opprettBehandling(it)
        }

        val behandling = behandlingRepo.hentBehandling(id = opprettBehandling.id)
        assertTrue(behandling is Revurdering)
    }

    @Test
    fun `skal returnere behandling av type ManueltOpphoer`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
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
        val behandling = behandlingRepo.hentBehandling(opprettBehandling.id)
        assertTrue(behandling is ManueltOpphoer)
    }

    @Test
    fun `skal returnere liste med behandlinger av ulike typer`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        repeat(2) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.REVURDERING,
                    sakId = sak1,
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    prosesstype = Prosesstype.AUTOMATISK
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

        val behandlinger = behandlingRepo.alleBehandlingerISak(sak1)
        assertAll(
            "Skal hente ut to foerstegangsbehandlinger og to revurderinger",
            { assertEquals(4, behandlinger.size) },
            { assertEquals(2, behandlinger.filterIsInstance<Revurdering>().size) },
            { assertEquals(2, behandlinger.filterIsInstance<Foerstegangsbehandling>().size) }
        )
    }

    @Test
    fun `Skal bare hente behandlinger av en gitt type`() {
        val sak1 = sakRepo.opprettSak("1234", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        repeat(2) {
            behandlingRepo.opprettBehandling(
                opprettBehandling(
                    type = BehandlingType.REVURDERING,
                    sakId = sak1,
                    revurderingAarsak = RevurderingAarsak.REGULERING,
                    prosesstype = Prosesstype.MANUELL
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

        val foerstegangsbehandlinger = behandlingRepo.alleBehandlingerISak(sak1).filter {
            it.type == BehandlingType.FØRSTEGANGSBEHANDLING
        }
        val revurderinger = behandlingRepo.alleBehandlingerISak(sak1).filter { it.type == BehandlingType.REVURDERING }
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
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak1).also {
            behandlingRepo.opprettBehandling(it)
        }

        val behandlingFoerStatusendring = behandlingRepo.hentBehandling(opprettBehandling.id) as? Foerstegangsbehandling
        assertNotNull(behandlingFoerStatusendring)

        val endretTidspunkt = Tidspunkt.now().toLocalDatetimeUTC()
        val behandlingMedNyStatus =
            behandlingFoerStatusendring!!.copy(status = BehandlingStatus.VILKAARSVURDERT, sistEndret = endretTidspunkt)
        behandlingRepo.lagreStatus(behandlingMedNyStatus)

        val behandlingEtterStatusendring = behandlingRepo.hentBehandling(opprettBehandling.id)

        assertEquals(BehandlingStatus.OPPRETTET, behandlingFoerStatusendring.status)
        assertEquals(BehandlingStatus.VILKAARSVURDERT, behandlingEtterStatusendring!!.status)
        assertEquals(endretTidspunkt, behandlingEtterStatusendring.sistEndret)
    }

    @Test
    fun `skal hente alle loepende behandlinger i en sak`() {
        val sak1 = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id

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
        val alleLoependeBehandlinger =
            behandlingRepo.alleBehandlingerISak(sak1).filter { it.status in BehandlingStatus.underBehandling() }
        assertEquals(4, lagredeBehandlinger.size)
        assertEquals(2, alleLoependeBehandlinger.size)
    }

    @Test
    fun `skal lagre virkningstidspunkt for en behandling`() {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak,
            status = BehandlingStatus.OPPRETTET
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val behandling = behandlingRepo.hentBehandling(opprettBehandling.id) as? Foerstegangsbehandling

        assertNotNull(behandling)

        val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("navIdent")
        val nyDato = YearMonth.of(2021, 2)
        behandling!!.oppdaterVirkningstidspunkt(Virkningstidspunkt(nyDato, saksbehandler, "enBegrunnelse")).let {
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
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
        val opprettBehandling = opprettBehandling(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            sakId = sak,
            status = BehandlingStatus.OPPRETTET
        )

        behandlingRepo.opprettBehandling(opprettBehandling)

        val kommerBarnetTilgode = KommerBarnetTilgode(
            JaNei.JA,
            "begrunnelse",
            Grunnlagsopplysning.Saksbehandler.create("navIdent"),
            opprettBehandling.id
        )
        kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(kommerBarnetTilgode)

        with(behandlingRepo.hentBehandling(opprettBehandling.id)) {
            val expected = kommerBarnetTilgode
            val actual = (this as Foerstegangsbehandling).kommerBarnetTilgode

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `kan oppdatere og lagre ny status for behandling`() {
        val sak = sakRepo.opprettSak("123", SakType.BARNEPENSJON, Enheter.defaultEnhet.enhetNr).id
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
        val saksbehandler = Grunnlagsopplysning.Saksbehandler.create("saksbehandler01")
        val kommerBarnetTilgode = KommerBarnetTilgode(JaNei.JA, "", saksbehandler, opprettBehandling.id)
        kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(kommerBarnetTilgode)

        val foerstegangsbehandling = behandlingRepo.hentBehandling(opprettBehandling.id) as? Foerstegangsbehandling

        assertNotNull(foerstegangsbehandling)

        val virkningstidspunkt = Virkningstidspunkt(YearMonth.of(2021, 1), saksbehandler, "")
        val gyldighetsResultat = GyldighetsResultat(
            VurderingsResultat.OPPFYLT,
            listOf(),
            Tidspunkt.now().toLocalDatetimeUTC()
        )

        foerstegangsbehandling!!
            .oppdaterVirkningstidspunkt(virkningstidspunkt)
            .oppdaterGyldighetsproeving(gyldighetsResultat)
            .tilVilkaarsvurdert()
            .let {
                behandlingRepo.lagreNyttVirkningstidspunkt(it.id, it.virkningstidspunkt!!)
                behandlingRepo.lagreGyldighetsproving(it)
                kommerBarnetTilGodeDao.lagreKommerBarnetTilGode(it.kommerBarnetTilgode!!)
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