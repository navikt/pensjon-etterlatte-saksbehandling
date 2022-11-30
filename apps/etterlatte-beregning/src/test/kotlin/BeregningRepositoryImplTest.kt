import io.mockk.mockk
import io.mockk.mockkClass
import no.nav.etterlatte.BeregningRepository
import no.nav.etterlatte.BeregningRepositoryImpl
import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
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
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

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
        val behandlingId = UUID.randomUUID()
        val beregningResultat = BeregningService(
            beregningRepository,
            mockk(),
            mockk(),
            mockk(),
            sendToRapid
        )
            .lagBeregning(
                opplysningsgrunnlag,
                YearMonth.of(2021, 2),
                YearMonth.of(2021, 9),
                mockkClass(VilkaarsvurderingUtfall::class),
                BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingId
            )

        val beregning = Beregning(
            beregningId = beregningResultat.beregningId,
            behandlingId = behandlingId,
            beregningsperioder = beregningResultat.beregningsperioder,
            beregnetDato = beregningResultat.beregnetDato,
            grunnlagMetadata = Metadata(
                sakId = 0,
                versjon = beregningResultat.grunnlagMetadata.versjon
            )
        )
        val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)

        assertEquals(beregning, lagretBeregning)
    }

    @Test
    fun `det som hentes ut skal være likt det som originalt ble lagret`() {
        val opplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val behandlingId = UUID.randomUUID()
        val beregningResultat = BeregningService(
            beregningRepository,
            mockk(),
            mockk(),
            mockk(),
            sendToRapid
        )
            .lagBeregning(
                opplysningsgrunnlag,
                YearMonth.of(2021, 2),
                YearMonth.of(2021, 9),
                mockkClass(VilkaarsvurderingUtfall::class),
                BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingId
            )

        val beregningLagret = Beregning(
            beregningId = beregningResultat.beregningId,
            behandlingId = behandlingId,
            beregningsperioder = beregningResultat.beregningsperioder,
            beregnetDato = beregningResultat.beregnetDato,
            grunnlagMetadata = Metadata(
                sakId = 0,
                versjon = beregningResultat.grunnlagMetadata.versjon
            )
        )
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)

        val beregningHentet = beregningRepository.hent(behandlingId)

        assertEquals(beregningLagret, beregningHentet)
    }

    @Test
    fun `skal oppdatere og eller lagre beregning`() {
        val opplysningsgrunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val behandlingId = UUID.randomUUID()
        val beregningResultat = BeregningService(
            beregningRepository,
            mockk(),
            mockk(),
            mockk(),
            sendToRapid
        ).lagBeregning(
            opplysningsgrunnlag,
            YearMonth.of(2021, 2),
            YearMonth.of(2021, 9),
            mockkClass(VilkaarsvurderingUtfall::class),
            BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingId
        )

        val beregningLagret = Beregning(
            beregningId = beregningResultat.beregningId,
            behandlingId = behandlingId,
            beregningsperioder = beregningResultat.beregningsperioder,
            beregnetDato = beregningResultat.beregnetDato,
            grunnlagMetadata = Metadata(
                sakId = opplysningsgrunnlag.metadata.sakId,
                versjon = beregningResultat.grunnlagMetadata.versjon
            )
        )
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)

        val beregningHentet = beregningRepository.hent(behandlingId)

        assertEquals(beregningLagret, beregningHentet)

        val nyBeregning = BeregningService(beregningRepository, mockk(), mockk(), mockk(), sendToRapid).lagBeregning(
            opplysningsgrunnlag,
            YearMonth.of(2021, 2),
            YearMonth.of(2024, 12),
            mockkClass(VilkaarsvurderingUtfall::class),
            BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingId
        )

        val nyBeregningMapped = Beregning(
            beregningId = nyBeregning.beregningId,
            behandlingId = behandlingId,
            beregningsperioder = nyBeregning.beregningsperioder,
            beregnetDato = nyBeregning.beregnetDato,
            grunnlagMetadata = Metadata(
                sakId = opplysningsgrunnlag.metadata.sakId,
                versjon = nyBeregning.grunnlagMetadata.versjon + 1
            )
        )
        beregningRepository.lagreEllerOppdaterBeregning(nyBeregningMapped)
        val beregningHentetNy = beregningRepository.hent(behandlingId)

        assertEquals(nyBeregningMapped, beregningHentetNy)
    }
}