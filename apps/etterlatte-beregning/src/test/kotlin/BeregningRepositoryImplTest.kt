import io.mockk.mockk
import io.mockk.mockkClass
import no.nav.etterlatte.BeregningRepository
import no.nav.etterlatte.BeregningRepositoryImpl
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.model.Beregning
import no.nav.etterlatte.model.BeregningService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
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

    private fun initiellBeregning(
        opplysningsgrunnlag: Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
    ): Beregning {
        return BeregningService(
            beregningRepository,
            mockk(),
            mockk(),
            mockk()
        ).lagBeregning(
            grunnlag = opplysningsgrunnlag,
            virkFOM = YearMonth.of(2021, 2),
            virkTOM = YearMonth.of(2021, 9),
            vilkaarsvurderingUtfall = mockkClass(VilkaarsvurderingUtfall::class),
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingId = UUID.randomUUID()
        )
    }

    @Test
    fun `lagre() skal returnere samme data som faktisk ble lagret`() {
        val beregning = initiellBeregning()
        val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)

        assertEquals(beregning, lagretBeregning)
    }

    @Test
    fun `det som hentes ut skal være likt det som originalt ble lagret`() {
        val beregningLagret = initiellBeregning()
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)

        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(beregningLagret, beregningHentet)
    }

    @Test
    fun `skal oppdatere og eller lagre beregning`() {
        val beregningLagret = initiellBeregning()

        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)
        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(beregningLagret, beregningHentet)

        val opplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

        val nyBeregning = BeregningService(beregningRepository, mockk(), mockk(), mockk()).lagBeregning(
            grunnlag = opplysningsgrunnlag,
            virkFOM = YearMonth.of(2021, 2),
            virkTOM = YearMonth.of(2024, 12),
            vilkaarsvurderingUtfall = mockkClass(VilkaarsvurderingUtfall::class),
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingId = beregningLagret.behandlingId
        ).copy(
            grunnlagMetadata = Metadata(
                sakId = beregningLagret.grunnlagMetadata.sakId,
                versjon = beregningLagret.grunnlagMetadata.versjon + 1
            )
        )
        beregningRepository.lagreEllerOppdaterBeregning(nyBeregning)
        val beregningHentetNy = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(nyBeregning, beregningHentetNy)
    }

    @Test
    fun `skal slette alle beregningperioder basert på sakId`() {
        val beregning = initiellBeregning()
        val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)

        assertDoesNotThrow { beregningRepository.hent(beregning.behandlingId) }

        beregningRepository.slettBeregningsperioderISak(lagretBeregning.grunnlagMetadata.sakId)

        val emptyBeregning = beregningRepository.hent(beregning.behandlingId)
        assertNull(emptyBeregning)
    }
}