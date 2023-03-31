package no.nav.etterlatte.trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import trygdetid.beregnetTrygdetid
import trygdetid.trygdetidGrunnlag
import java.util.*
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidRepositoryTest {

    @Container
    private val postgres = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var repository: TrygdetidRepository
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgres.start()
        dataSource = DataSourceBuilder.createDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        repository = TrygdetidRepository(dataSource.apply { migrate() })
    }

    @AfterAll
    fun afterAll() {
        postgres.stop()
    }

    @AfterEach
    fun afterEach() {
        cleanDatabase()
    }

    @Test
    fun `skal opprette trygdetid`() {
        val behandlingId = randomUUID()

        val trygdetid = repository.opprettTrygdetid(behandlingId)

        trygdetid shouldNotBe null
        trygdetid.behandlingId shouldBe behandlingId
    }

    @Test
    fun `skal opprette og hente trygdetid`() {
        val behandlingId = randomUUID()
        repository.opprettTrygdetid(behandlingId)

        val trygdetid = repository.hentTrygdetid(behandlingId)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandlingId
    }

    @Test
    fun `skal opprette et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetid = repository.opprettTrygdetid(behandlingId)
        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)

        val trygdetidMedTrygdetidGrunnlag =
            repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)

        trygdetidMedTrygdetidGrunnlag shouldNotBe null
        with(trygdetidMedTrygdetidGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe trygdetidGrunnlag
        }
    }

    @Test
    fun `skal oppdatere et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetid = repository.opprettTrygdetid(behandlingId)

        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)

        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(trygdetid = 20)
        val trygdetidMedOppdatertGrunnlag =
            repository.oppdaterTrygdetidGrunnlag(behandlingId, endretTrygdetidGrunnlag)

        trygdetidMedOppdatertGrunnlag shouldNotBe null
        with(trygdetidMedOppdatertGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe endretTrygdetidGrunnlag
        }
    }

    @Test
    fun `skal hente et trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetid = repository.opprettTrygdetid(behandlingId)

        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        repository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)

        val hentetTrygdetidGrunnlag = repository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id)

        hentetTrygdetidGrunnlag shouldNotBe null
    }

    @Test
    fun `skal oppdatere beregnet trygdetid`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(nasjonal = 10, fremtidig = 2, total = 12)
        repository.opprettTrygdetid(behandlingId)

        val trygdetidMedBeregnetTrygdetid = repository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)

        trygdetidMedBeregnetTrygdetid shouldNotBe null
        trygdetidMedBeregnetTrygdetid.beregnetTrygdetid shouldBe beregnetTrygdetid
    }

    private fun cleanDatabase() {
        dataSource.connection.use { it.prepareStatement("TRUNCATE trygdetid CASCADE").apply { execute() } }
    }
}