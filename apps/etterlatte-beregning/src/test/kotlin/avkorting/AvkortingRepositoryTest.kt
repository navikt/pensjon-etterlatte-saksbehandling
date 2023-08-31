package avkorting

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.Aarsoppgjoer
import no.nav.etterlatte.avkorting.AvkortetYtelseType
import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.avkorting.AvkortingRepository
import no.nav.etterlatte.avkorting.Inntektsavkorting
import no.nav.etterlatte.beregning.regler.aarsoppgjoer
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.beregning.regler.avkortinggrunnlag
import no.nav.etterlatte.beregning.regler.avkortingsperiode
import no.nav.etterlatte.beregning.regler.ytelseFoerAvkorting
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
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
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
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
        val grunnlag = avkortinggrunnlag()
        val aarsoppgjoer = Aarsoppgjoer(
            ytelseFoerAvkorting = listOf(ytelseFoerAvkorting()),
            inntektsavkorting = listOf(
                Inntektsavkorting(
                    grunnlag = grunnlag,
                    avkortingsperioder = listOf(avkortingsperiode(inntektsgrunnlag = grunnlag.id)),
                    avkortetYtelse = listOf(avkortetYtelse(type = AvkortetYtelseType.INNTEKT, inntektsgrunnlag = grunnlag.id)),
                )
            ),
            avkortetYtelseAar = listOf(avkortetYtelse(type = AvkortetYtelseType.AARSOPPGJOER)),
            avkortetYtelseForrigeVedtak = listOf(avkortetYtelse(type = AvkortetYtelseType.FORRIGE_VEDTAK)),
        )

        avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                aarsoppgjoer = aarsoppgjoer,
            )
        )
        val endretGrunnlag = aarsoppgjoer.inntektsavkorting[0].grunnlag.copy(spesifikasjon = "Endret")
        val endretAvkortingsperioder =
            aarsoppgjoer.inntektsavkorting[0].avkortingsperioder.map { it.copy(avkorting = 333) }
        val endretAvkortetYtelse =
            aarsoppgjoer.inntektsavkorting[0].avkortetYtelse.map { it.copy(avkortingsbeloep = 444) }

        val endretYtelseFoerAvkorting = listOf(aarsoppgjoer.ytelseFoerAvkorting[0].copy(beregning = 333))
        val endretInntektsavkorting = listOf(
            Inntektsavkorting(
                grunnlag = endretGrunnlag,
                avkortingsperioder = endretAvkortingsperioder,
                avkortetYtelse = endretAvkortetYtelse
            )
        )
        val endretAvkortetYtelseAar = aarsoppgjoer.avkortetYtelseAar.map { it.copy(avkortingsbeloep = 444) }
        val endretAvkortetYtelseForrigeVedtak =
            aarsoppgjoer.avkortetYtelseForrigeVedtak.map { it.copy(avkortingsbeloep = 444) }

        val avkorting = avkortingRepository.lagreAvkorting(
            behandlingId,
            Avkorting(
                aarsoppgjoer = aarsoppgjoer(
                    ytelseFoerAvkorting = endretYtelseFoerAvkorting,
                    inntektsavkorting = endretInntektsavkorting,
                    avkortetYtelseAar = endretAvkortetYtelseAar,
                    avkortetYtelseForrigeVedtak = endretAvkortetYtelseForrigeVedtak
                ),
            )
        )

        with(avkorting.aarsoppgjoer) {
            ytelseFoerAvkorting.asClue {
                it.size shouldBe 1
                it shouldBe endretYtelseFoerAvkorting
            }
            inntektsavkorting.asClue {
                it.size shouldBe 1
                it[0].asClue { avkorting ->
                    avkorting.grunnlag shouldBe endretGrunnlag
                    avkorting.avkortingsperioder shouldBe endretAvkortingsperioder
                    avkorting.avkortetYtelse shouldBe endretAvkortetYtelse
                }
            }
            avkortetYtelseAar.asClue {
                it.size shouldBe 1
                it shouldBe endretAvkortetYtelseAar
            }
            avkortetYtelseForrigeVedtak.asClue {
                it.size shouldBe 1
                it shouldBe endretAvkortetYtelseForrigeVedtak
            }
        }
    }

    @Test
    fun `fyller lopende ytlisning basert paa aarets oppgjoet`() {
        // TODO EY-2523
    }

}