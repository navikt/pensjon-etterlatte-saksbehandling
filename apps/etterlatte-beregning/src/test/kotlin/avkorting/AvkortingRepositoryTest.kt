package avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.restanse
import no.nav.etterlatte.beregning.regler.ytelseFoerAvkorting
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
        val aarsoppgjoer = Aarsoppgjoer(
            ytelseFoerAvkorting = listOf(ytelseFoerAvkorting()),
            avkortingsperioder = listOf(avkortingsperiode()),
            tidligereAvkortetYtelse = listOf(avkortetYtelse(type = AvkortetYtelseType.TIDLIGERE)),
            reberegnetAvkortetYtelse = listOf(avkortetYtelse(type = AvkortetYtelseType.REBEREGNET)),
            restanse = restanse()
        )
        val avkortetYtelse = listOf(avkortetYtelse())

        avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                avkortingGrunnlag = avkortinggrunnlag,
                aarsoppgjoer = aarsoppgjoer,
                avkortetYtelse = avkortetYtelse,
            )
        )

        val endretAvkortingGrunnlag = listOf(avkortinggrunnlag[0].copy(spesifikasjon = "Endret"))
        val endretYtelseFoerAvkorting = listOf(aarsoppgjoer.ytelseFoerAvkorting[0].copy(beregning = 333))
        val endretAvkortingsperiode = listOf(aarsoppgjoer.avkortingsperioder[0].copy(avkorting = 333))
        val endretTidligereAvkortetYtelse = listOf(aarsoppgjoer.tidligereAvkortetYtelse[0].copy(avkortingsbeloep = 444))
        val endretReberegnetYtelse = listOf(aarsoppgjoer.reberegnetAvkortetYtelse[0].copy(avkortingsbeloep = 444))
        val endretRestanse = aarsoppgjoer.restanse!!.copy(totalRestanse = 333)
        val endretAvkortetYtelse = listOf(avkortetYtelse[0].copy(avkortingsbeloep = 444, restanse = 100))

        val avkorting = avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                avkortingGrunnlag = endretAvkortingGrunnlag,
                aarsoppgjoer = aarsoppgjoer(
                    ytelseFoerAvkorting = endretYtelseFoerAvkorting,
                    avkortingsperioder = endretAvkortingsperiode,
                    tidligereAvkortetYtelse = endretTidligereAvkortetYtelse,
                    reberegnetAvkortetYtelse = endretReberegnetYtelse,
                    restanse = endretRestanse
                ),
                avkortetYtelse = endretAvkortetYtelse
            )
        )

        avkorting.avkortingGrunnlag.asClue {
            it.size shouldBe 1
            it shouldBe endretAvkortingGrunnlag
        }
        with(avkorting.aarsoppgjoer) {
            avkortingsperioder.asClue {
                it.size shouldBe 1
                it shouldBe endretAvkortingsperiode
            }
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe endretYtelseFoerAvkorting
            }
            tidligereAvkortetYtelse.asClue {
                it.size shouldBe 1
                it shouldBe endretTidligereAvkortetYtelse
            }
            reberegnetAvkortetYtelse.asClue {
                it.size shouldBe 1
                it shouldBe endretReberegnetYtelse
            }
            restanse.asClue {
                it shouldBe endretRestanse
            }
        }
        avkorting.avkortetYtelse.asClue {
            it.size shouldBe 1
            it shouldBe endretAvkortetYtelse
        }
    }
}