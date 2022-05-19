package no.nav.etterlatte.itest

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.behandling.*
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlagTyper.InnsenderErForelderGrunnlag
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.sak.SakDao
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")

    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal opprette behandling og legge til persongalleri`() {
        val connection = dataSource.connection
        val sakrepo = SakDao { connection }
        val behandlingRepo = BehandlingDao { connection }
        val sak1 = sakrepo.opprettSak("123", "BP").id
        val behandlingOpprettet = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)

        val behandlingUtenPersongalleri = Behandling(
            UUID.randomUUID(),
            sak1,
            behandlingOpprettet,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            null,
            BehandlingStatus.OPPRETTET
        )

        behandlingRepo.opprett(behandlingUtenPersongalleri)
        val opprettetBehandling = requireNotNull(behandlingRepo.hentBehandling(behandlingUtenPersongalleri.id))
        Assertions.assertEquals(behandlingUtenPersongalleri.id, opprettetBehandling.id)
        Assertions.assertEquals(behandlingUtenPersongalleri.avdoed, opprettetBehandling.avdoed)
        Assertions.assertEquals(behandlingUtenPersongalleri.soesken, opprettetBehandling.soesken)
        Assertions.assertEquals(
            behandlingUtenPersongalleri.behandlingOpprettet,
            opprettetBehandling.behandlingOpprettet
        )

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            listOf("søster", "bror"),
            listOf("Avdoed"),
            listOf("Gjenlevende"),
        )

        val persongalleriBehandling = opprettetBehandling.copy(
            sistEndret = LocalDateTime.now(),
            soeknadMottattDato = LocalDateTime.parse(LocalDateTime.now().toString()),
            innsender = persongalleri.innsender,
            soeker = persongalleri.soeker,
            gjenlevende = persongalleri.gjenlevende,
            avdoed = persongalleri.avdoed,
            soesken = persongalleri.soesken,
        )

        behandlingRepo.lagrePersongalleriOgMottattdato(persongalleriBehandling)
        val lagretPersongalleriBehandling = requireNotNull(behandlingRepo.hentBehandling(persongalleriBehandling.id))

        Assertions.assertEquals(persongalleriBehandling.id, lagretPersongalleriBehandling.id)
        Assertions.assertEquals(persongalleriBehandling.avdoed, lagretPersongalleriBehandling.avdoed)
        Assertions.assertEquals(persongalleriBehandling.soesken, lagretPersongalleriBehandling.soesken)
        Assertions.assertEquals(
            persongalleriBehandling.behandlingOpprettet,
            lagretPersongalleriBehandling.behandlingOpprettet
        )

        connection.close()
    }

    @Test
    fun `Skal legge til gyldighetsprøving til en opprettet behandling`() {
        val connection = dataSource.connection
        val sakrepo = SakDao { connection }
        val behandlingRepo = BehandlingDao { connection }
        val sak1 = sakrepo.opprettSak("123", "BP").id

        val behandlingUtenPersongalleri = Behandling(
            UUID.randomUUID(),
            sak1,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            null,
            BehandlingStatus.OPPRETTET
        )

        behandlingRepo.opprett(behandlingUtenPersongalleri)

        val persongalleri = Persongalleri(
            "Soeker",
            "Innsender",
            listOf("søster", "bror"),
            listOf("Avdoed"),
            listOf("Gjenlevende"),
        )

        val persongalleriBehandling = behandlingUtenPersongalleri.copy(
            sistEndret = LocalDateTime.now(),
            soeknadMottattDato = LocalDateTime.parse(LocalDateTime.now().toString()),
            innsender = persongalleri.innsender,
            soeker = persongalleri.soeker,
            gjenlevende = persongalleri.gjenlevende,
            avdoed = persongalleri.avdoed,
            soesken = persongalleri.soesken,
        )
        behandlingRepo.lagrePersongalleriOgMottattdato(persongalleriBehandling)
        val lagretPersongalleriBehandling = requireNotNull(behandlingRepo.hentBehandling(persongalleriBehandling.id))

        val gyldighetsproevingBehanding = lagretPersongalleriBehandling.copy(
            gyldighetsproeving = GyldighetsResultat(
                resultat = VurderingsResultat.OPPFYLT,
                vurderinger = listOf(VurdertGyldighet(
                    navn = GyldighetsTyper.INNSENDER_ER_FORELDER,
                    resultat = VurderingsResultat.OPPFYLT,
                    basertPaaOpplysninger = InnsenderErForelderGrunnlag(null, null, null)
                )),
                vurdertDato = LocalDateTime.now()
            ),
            status = BehandlingStatus.GYLDIG_SOEKNAD
        )

        behandlingRepo.lagreGyldighetsproving(gyldighetsproevingBehanding)
        val lagretGyldighetsproving = requireNotNull(behandlingRepo.hentBehandling(persongalleriBehandling.id))

        Assertions.assertEquals(gyldighetsproevingBehanding.gyldighetsproeving, lagretGyldighetsproving.gyldighetsproeving)
    }

    @Test
    fun `Sletting av alle behandlinger i en sak`() {
        val connection = dataSource.connection
        val sakrepo = SakDao { connection }
        val behandlingRepo = BehandlingDao { connection }

        val sak1 = sakrepo.opprettSak("123", "BP").id
        val sak2 = sakrepo.opprettSak("321", "BP").id

        listOf(
            Behandling(
                UUID.randomUUID(),
                sak1,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                BehandlingStatus.OPPRETTET
            ),
            Behandling(
                UUID.randomUUID(),
                sak1,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                BehandlingStatus.OPPRETTET
            ),
            Behandling(
                UUID.randomUUID(),
                sak2,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                BehandlingStatus.OPPRETTET
            ),
        ).forEach { b ->
            behandlingRepo.opprett(b)
        }

        Assertions.assertEquals(2, behandlingRepo.alleBehandingerISak(sak1).size)
        behandlingRepo.slettBehandlingerISak(sak1)
        Assertions.assertEquals(0, behandlingRepo.alleBehandingerISak(sak1).size)
        Assertions.assertEquals(1, behandlingRepo.alleBehandingerISak(sak2).size)

        connection.close()
    }


    @Test
    fun `avbryte sak`() {
        val connection = dataSource.connection
        val sakrepo = SakDao { connection }
        val behandlingRepo = BehandlingDao { connection }

        val sak1 = sakrepo.opprettSak("123", "BP").id
        listOf(
            Behandling(
                UUID.randomUUID(),
                sak1,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                emptyList(),
                emptyList(),
                emptyList(),
                null,
                BehandlingStatus.OPPRETTET
            )
        ).forEach { b ->
            behandlingRepo.opprett(b)
        }

        var behandling = behandlingRepo.alleBehandingerISak(sak1)
        Assertions.assertEquals(1, behandling.size)
        Assertions.assertEquals(false, behandling.first().status == BehandlingStatus.AVBRUTT)

        behandlingRepo.avbrytBehandling(behandling.first())
        behandling = behandlingRepo.alleBehandingerISak(sak1)
        Assertions.assertEquals(1, behandling.size)
        Assertions.assertEquals(true, behandling.first().status == BehandlingStatus.AVBRUTT)

        connection.close()
    }

}
