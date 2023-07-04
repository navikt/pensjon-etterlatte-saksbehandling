package avkorting

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.Restanse
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
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
        val avkortinggrunnlag = listOf(avkortinggrunnlag())
        val avkortingsperioder = listOf(avkortingsperiode())
        val avkortetYtelse = listOf(avkortetYtelse())

        avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                avkortingGrunnlag = avkortinggrunnlag,
                aarsoppgjoer = aarsoppgjoer(avkortingsperioder = avkortingsperioder,),
                avkortetYtelse = avkortetYtelse,
            )
        )

        val endretAvkortingGrunnlag = listOf(avkortinggrunnlag[0].copy(spesifikasjon = "Endret"))
        val endretAvkortingsperiode = listOf(avkortingsperioder[0].copy(avkorting = 333))
        val endretAvkortetYtelse = listOf(
            avkortetYtelse[0].copy(
                avkortingsbeloep = 444,
                restanse = 100
            )
        )

        val avkorting = avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                avkortingGrunnlag = endretAvkortingGrunnlag,
                aarsoppgjoer = aarsoppgjoer(avkortingsperioder = endretAvkortingsperiode),
                avkortetYtelse = endretAvkortetYtelse
            )
        )

        avkorting.avkortingGrunnlag.size shouldBe 1
        avkorting.avkortingGrunnlag shouldBe endretAvkortingGrunnlag
        avkorting.aarsoppgjoer.avkortingsperioder shouldBe endretAvkortingsperiode
        avkorting.avkortetYtelse shouldBe endretAvkortetYtelse
    }
}