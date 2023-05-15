package beregning

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.beregning.AvkortingGrunnlag
import no.nav.etterlatte.beregning.AvkortingRepository
import no.nav.etterlatte.beregning.BeregnetAvkortingGrunnlag
import no.nav.etterlatte.beregning.regler.avkortetYtelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
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

    companion object {
        val behandlingId: UUID = UUID.randomUUID()
        val avkortinggrunnlag = AvkortingGrunnlag(
            periode = Periode(fom = YearMonth.now(), tom = null),
            aarsinntekt = 500000,
            spesifikasjon = "Grunnlag før endring",
            kilde = Grunnlagsopplysning.Saksbehandler.create("Z123456"),
            beregnetAvkorting = listOf(
                BeregnetAvkortingGrunnlag(
                    periode = Periode(fom = YearMonth.now(), tom = null),
                    avkorting = 100,
                    tidspunkt = Tidspunkt.now(),
                    regelResultat = "".toJsonNode(),
                    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1")
                )
            )
        )
    }

    @Test
    fun `Skal lagre eller oppdatere avkortinggrunnlag`() {
        val endretAvkortningGrunnlag = avkortinggrunnlag.copy(spesifikasjon = "Endret grunnlag")

        avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, avkortinggrunnlag)
        val avkortning = avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, endretAvkortningGrunnlag)

        with(avkortning) {
            this.behandlingId shouldBe behandlingId
            avkortingGrunnlag.size shouldBe 1
            avkortingGrunnlag[0] shouldBe endretAvkortningGrunnlag
        }
    }

    @Test
    fun `Skal lagre eller oppdatere avkortet ytelse`() {
        val avkortetYtelse = avkortetYtelse()
        val endretAvkortetYtelse = avkortetYtelse.copy(ytelseEtterAvkorting = 200)

        avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, avkortinggrunnlag)

        avkortingRepository.lagreEllerOppdaterAvkortetYtelse(behandlingId, listOf(avkortetYtelse))
        val avkortning = avkortingRepository.lagreEllerOppdaterAvkortetYtelse(
            behandlingId,
            listOf(endretAvkortetYtelse)
        )

        with(avkortning) {
            this.behandlingId shouldBe behandlingId
            this.avkortetYtelse.size shouldBe 1
            this.avkortetYtelse[0] shouldBe endretAvkortetYtelse
        }
    }
}