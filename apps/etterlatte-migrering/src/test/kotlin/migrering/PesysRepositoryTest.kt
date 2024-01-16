package migrering

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.Migreringsstatus
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.Pesyssak
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PesysRepositoryTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
    private lateinit var repository: PesysRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        val ds =
            DataSourceBuilder.createDataSource(
                postgreSQLContainer.jdbcUrl,
                postgreSQLContainer.username,
                postgreSQLContainer.password,
            ).also { it.migrate() }

        repository = PesysRepository(ds)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `lagre kobling til behandlingid`() {
        val behandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), behandlingId)

        val kobling = repository.hentKoplingTilBehandling(PesysId(123L))

        assertEquals(behandlingId, kobling!!.behandlingId)
    }

    @Test
    fun `Skal oppdatere migreringsstatus til ferdig naar brevdistribusjon er ferdig foerst`() {
        val behandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), behandlingId)

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.BREVUTSENDING_OK)
        assertEquals(Migreringsstatus.BREVUTSENDING_OK, repository.hentStatus(123L))

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.UTBETALING_OK)
        assertEquals(Migreringsstatus.FERDIG, repository.hentStatus(123L))
    }

    @Test
    fun `Skal oppdatere migreringsstatus til ferdig naar utbetaling er godkjent foerst`() {
        val behandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), behandlingId)

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.UTBETALING_OK)
        assertEquals(Migreringsstatus.UTBETALING_OK, repository.hentStatus(123L))

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.BREVUTSENDING_OK)
        assertEquals(Migreringsstatus.FERDIG, repository.hentStatus(123L))
    }

    @Test
    fun `lagre kobling til behandlingid oppdateres til ny behandlingsid`() {
        sakMedKobling(pesysSak(123L), UUID.randomUUID())
        val nyBehandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), nyBehandlingId)

        val nyKobling = repository.hentKoplingTilBehandling(PesysId(123L))

        assertEquals(nyBehandlingId, nyKobling!!.behandlingId)
    }

    @Test
    fun `lagre manuell migrering`() {
        val pesysId = 123L

        repository.lagreManuellMigrering(pesysId)
        val manuellMigrering = repository.hentStatus(pesysId)

        assertEquals(Migreringsstatus.UNDER_MIGRERING_MANUELT, manuellMigrering)
    }

    @Test
    fun `Lagre gyldige dry runs flere ganger`() {
        val pesyssak = pesysSak(123L)
        repository.lagrePesyssak(pesyssak)
        repository.lagreGyldigDryRun(pesyssak.tilMigreringsrequest())
        repository.lagreGyldigDryRun(pesyssak.tilMigreringsrequest())
    }

    private fun sakMedKobling(
        pesyssak: Pesyssak,
        behandlingsId: UUID,
    ) {
        repository.lagrePesyssak(pesyssak)
        repository.oppdaterStatus(PesysId(pesyssak.id), Migreringsstatus.UNDER_MIGRERING)
        repository.lagreKoplingTilBehandling(behandlingsId, PesysId(pesyssak.id))
    }

    companion object {
        private fun pesysSak(id: Long) =
            Pesyssak(
                id = id,
                enhet = Enhet("enhet"),
                soeker = Folkeregisteridentifikator.of("09498230323"),
                gjenlevendeForelder = Folkeregisteridentifikator.of("09498230323"),
                avdoedForelder = listOf(AvdoedForelder(Folkeregisteridentifikator.of("09498230323"), Tidspunkt.now())),
                foersteVirkningstidspunkt = YearMonth.of(2024, 1),
                beregning = Beregning(0, 0, 0, Tidspunkt.now(), 0, IntBroek(0, 0)),
                trygdetid = Trygdetid(emptyList()),
                dodAvYrkesskade = false,
                flyktningStatus = false,
                spraak = Spraak.NB,
            )
    }
}
