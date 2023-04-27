package beregning

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.AvkortingGrunnlag
import no.nav.etterlatte.beregning.AvkortingRepository
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvkortingRepositoryTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var avkortingRepository: AvkortingRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        val ds = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }

        avkortingRepository = AvkortingRepository(ds)
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `Skal lagre eller oppdatere avkortinggrunnlag`() {
        val behandlingId = UUID.randomUUID()
        val avkortinggrunnlag = AvkortingGrunnlag(
            periode = Periode(fom = YearMonth.now(), tom = null),
            aarsinntekt = 500000,
            gjeldendeAar = 2023,
            spesifikasjon = "Grunnlag før endring"
        )
        val endretAvkortningGrunnlag = avkortinggrunnlag.copy(spesifikasjon = "Endret grunnlag")

        avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, avkortinggrunnlag)
        val avkortning = avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, endretAvkortningGrunnlag)

        with(avkortning) {
            this.behandlingId shouldBe behandlingId
            avkortingGrunnlag.size shouldBe 1
            avkortingGrunnlag[0] shouldBe endretAvkortningGrunnlag
        }
    }
}