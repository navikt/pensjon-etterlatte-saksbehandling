package vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksvurderingRepositoryTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")

    private lateinit var dataSource: DataSource
    private lateinit var vedtaksvurderingRepository: VedtaksvurderingRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }

        vedtaksvurderingRepository = VedtaksvurderingRepository(dataSource)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `kan hente alle vedtak knyttet til en sak`() {
        val sakId = 1L
        listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).forEach {
            vedtaksvurderingRepository.opprettVedtak(
                behandlingsId = it,
                sakid = sakId,
                fnr = "",
                saktype = SakType.BARNEPENSJON,
                behandlingtype = BehandlingType.FØRSTEGANGSBEHANDLING,
                virkningsDato = LocalDate.now(),
                beregningsresultat = null,
                vilkaarsresultat = null
            )
        }

        with(vedtaksvurderingRepository.hentVedtakForSak(1)) {
            Assertions.assertEquals(3, this.size)
        }
    }
}