import io.mockk.mockk
import io.mockk.mockkClass
import no.nav.etterlatte.BeregningRepository
import no.nav.etterlatte.BeregningRepositoryImpl
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaarsvurdering
import no.nav.etterlatte.model.Beregning
import no.nav.etterlatte.model.BeregningService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.YearMonth
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningRepositoryImplTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var beregningRepository: BeregningRepository

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        val ds = DataSourceBuilder(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }

        beregningRepository = BeregningRepositoryImpl(ds.dataSource())
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `lagre() skal returnere samme data som faktisk ble lagret`() {
        val opplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val beregningResultat = BeregningService(beregningRepository, mockk(), mockk(), mockk()).beregnResultat(
            opplysningsgrunnlag,
            YearMonth.of(2021, 2),
            YearMonth.of(2021, 9),
            mockkClass(Vilkaarsvurdering::class),
            BehandlingType.FØRSTEGANGSBEHANDLING
        )
        val behandlingId = UUID.randomUUID()

        val beregning = Beregning(
            beregningId = beregningResultat.id,
            behandlingId = behandlingId,
            beregningsperioder = beregningResultat.beregningsperioder,
            beregnetDato = beregningResultat.beregnetDato.toTidspunkt(norskTidssone),
            grunnlagMetadata = Metadata(
                sakId = 0,
                versjon = beregningResultat.grunnlagVersjon
            )
        )
        val lagretBeregning = beregningRepository.lagre(beregning)

        assertEquals(beregning, lagretBeregning)
    }

    @Test
    fun `det som hentes ut skal være likt det som originalt ble lagret`() {
        val opplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val beregningResultat = BeregningService(beregningRepository, mockk(), mockk(), mockk()).beregnResultat(
            opplysningsgrunnlag,
            YearMonth.of(2021, 2),
            YearMonth.of(2021, 9),
            mockkClass(Vilkaarsvurdering::class),
            BehandlingType.FØRSTEGANGSBEHANDLING
        )
        val behandlingId = UUID.randomUUID()

        val beregningLagret = Beregning(
            beregningId = beregningResultat.id,
            behandlingId = behandlingId,
            beregningsperioder = beregningResultat.beregningsperioder,
            beregnetDato = beregningResultat.beregnetDato.toTidspunkt(norskTidssone),
            grunnlagMetadata = Metadata(
                sakId = 0,
                versjon = beregningResultat.grunnlagVersjon
            )
        )
        beregningRepository.lagre(beregningLagret)

        val beregningHentet = beregningRepository.hent(behandlingId)

        assertEquals(beregningLagret, beregningHentet)
    }
}