package vedtaksvurdering

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.vedtaksvurdering.database.DataSourceBuilder
import no.nav.etterlatte.vedtaksvurdering.database.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import vilkaarsvurdering.VilkaarsvurderingTestData
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DBTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:12")

    private lateinit var dataSource: DataSource
    private val beregning = mockk<BeregningKlient>(relaxed = true)
    private val vilkaarsvurdering = mockk<VilkaarsvurderingKlient>(relaxed = true)
    private val behandling = mockk<BehandlingKlient>(relaxed = true)
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

    private val uuid = UUID.randomUUID()
    private val sakId = 123L
    private val accessToken = "accessToken"

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private fun lagreIverksattVedtak() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid
        )
        vedtaksvurderingService.lagreIverksattVedtak(uuid)
    }

    @Test
    fun testDB() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid
        )

        val beregningDTO = BeregningDTO(
            UUID.randomUUID(),
            uuid,
            listOf(),
            LocalDateTime.now().toTidspunkt(
                norskTidssone
            ),
            Metadata(1L, 1L)
        )
        coEvery { beregning.hentBeregning(any(), any()) } returns beregningDTO
        coEvery { behandling.hentBehandling(any(), any()) } returns DetaljertBehandling(
            uuid,
            sakId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            "1231245",
            null,
            null,
            null,
            null,
            null,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            null,
            null
        )
        coEvery { behandling.hentSak(any(), any()) } returns mockk<Sak>().apply {
            every { id } returns 1L
            every { ident } returns "ident"
            every { sakType } returns SakType.BARNEPENSJON
        }
        coEvery { vilkaarsvurdering.hentVilkaarsvurdering(any(), any()) } returns VilkaarsvurderingTestData
            .oppfylt.copy(
                behandlingId = uuid
            )
        coEvery { behandling.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandling.attester(any(), any(), any()) } returns true
        coEvery { behandling.underkjenn(any(), any(), any()) } returns true

        runBlocking {
            vedtaksvurderingService.opprettEllerOppdaterVedtak(uuid, "access")
        }

        val vedtaket: Vedtak? = vedtaksvurderingService.hentFellesvedtak(uuid)

        assert(vedtaket?.beregning != null)
        assert(vedtaket?.vilkaarsvurdering != null)
        assert(vedtaket?.sak?.id != null)
        Assertions.assertNotNull(vedtaket?.virk)

        runBlocking { vedtaksvurderingService.fattVedtak(uuid, "saksbehandler", accessToken) }
        val fattetVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertTrue(fattetVedtak?.vedtakFattet!!)
        Assertions.assertEquals(VedtakStatus.FATTET_VEDTAK, fattetVedtak.vedtakStatus)

        runBlocking { vedtaksvurderingService.underkjennVedtak(uuid, accessToken) }
        val underkjentVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertEquals(VedtakStatus.RETURNERT, underkjentVedtak?.vedtakStatus)

        runBlocking { vedtaksvurderingService.fattVedtak(uuid, "saksbehandler", accessToken) }

        runBlocking { vedtaksvurderingService.attesterVedtak(uuid, "attestant", accessToken) }
        val attestertVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertNotNull(attestertVedtak?.attestant)
        Assertions.assertNotNull(attestertVedtak?.datoattestert)
        Assertions.assertNotNull(attestertVedtak?.virkningsDato)
        Assertions.assertEquals(VedtakStatus.ATTESTERT, attestertVedtak?.vedtakStatus)

        lagreIverksattVedtak()
        val iverksattVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertEquals(VedtakStatus.IVERKSATT, iverksattVedtak?.vedtakStatus)

        vedtaksvurderingService.slettSak(sakId)
        Assertions.assertNull(vedtaksvurderingService.hentVedtak(uuid))
    }
}