package avkorting

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.*

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
    fun `skal returnere null hvis mangler avkorting`() {
        avkortingRepository.hentAvkorting(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `Skal lagre og oppdatere avkorting`() {
        val behandlingId: UUID = UUID.randomUUID()
        val avkortinggrunnlag = mutableListOf(avkortinggrunnlag())
        val avkortingsperioder = mutableListOf(avkortingsperiode())
        val avkortetYtelse = mutableListOf(avkortetYtelse())

        avkortingRepository.lagreAvkorting(
            Avkorting(
                behandlingId,
                avkortinggrunnlag,
                avkortingsperioder,
                avkortetYtelse
            )
        )

        val endretAvkortingGrunnlag = mutableListOf(avkortinggrunnlag[0].copy(spesifikasjon = "Endret"))
        val endretAvkortingsperiode = mutableListOf(avkortingsperioder[0].copy(avkorting = 333))
        val endretAvkortetYtelse = mutableListOf(avkortetYtelse[0].copy(avkortingsbeloep = 444))

        val avkorting = avkortingRepository.lagreAvkorting(
            Avkorting(
                behandlingId,
                endretAvkortingGrunnlag,
                endretAvkortingsperiode,
                endretAvkortetYtelse
            )
        )

        avkorting.behandlingId shouldBe behandlingId
        avkorting.avkortingGrunnlag.size shouldBe 1
        avkorting.avkortingGrunnlag shouldBe endretAvkortingGrunnlag
        avkorting.avkortingsperioder shouldBe endretAvkortingsperiode
        avkorting.avkortetYtelse shouldBe endretAvkortetYtelse
    }
}