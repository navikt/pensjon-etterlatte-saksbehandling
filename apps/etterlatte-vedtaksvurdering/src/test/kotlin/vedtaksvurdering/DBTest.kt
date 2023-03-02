package no.nav.etterlatte.vedtaksvurdering

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.vilkaarsvurdering.VilkaarsvurderingTestData
import no.nav.etterlatte.token.Bruker
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
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DBTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private val beregning = mockk<BeregningKlient>(relaxed = true)
    private val vilkaarsvurdering = mockk<VilkaarsvurderingKlient>(relaxed = true)
    private val behandling = mockk<BehandlingKlient>(relaxed = true)
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

    private val sakId = 123L
    private val bruker =
        Bruker.of(
            accessToken = "accessToken",
            oid = null,
            sub = null,
            saksbehandler = "saksbehandler"
        )

    private val saksbehandlereSecret = mapOf("saksbehandler" to "4808", "attestant" to "4808")

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            postgreSQLContainer.jdbcUrl,
            postgreSQLContainer.username,
            postgreSQLContainer.password
        ).also { it.migrate() }
    }

    private fun settOppService() = VedtaksvurderingService(
        VedtaksvurderingRepository(dataSource),
        beregning,
        vilkaarsvurdering,
        behandling,
        sendToRapid,
        saksbehandlereSecret
    )

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private fun lagreIverksattVedtak(behandlingid: UUID) {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid,
            saksbehandlereSecret
        )
        vedtaksvurderingService.lagreIverksattVedtak(behandlingid)
    }

    @Test
    fun testDB() {
        val vedtaksvurderingService = settOppService()
        val uuid = UUID.randomUUID().also { settOpp(it) }

        runBlocking {
            vedtaksvurderingService.opprettEllerOppdaterVedtak(
                uuid,
                Bruker.of("access", "1", null, null)
            )
        }

        val vedtaket: Vedtak? = vedtaksvurderingService.hentFellesvedtak(uuid)

        assert(vedtaket?.beregning != null)
        assert(vedtaket?.vilkaarsvurdering != null)
        assert(vedtaket?.sak?.id != null)
        Assertions.assertNotNull(vedtaket?.virk)

        runBlocking { vedtaksvurderingService.fattVedtak(uuid, bruker) }
        val fattetVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertTrue(fattetVedtak?.vedtakFattet!!)
        Assertions.assertEquals(VedtakStatus.FATTET_VEDTAK, fattetVedtak.vedtakStatus)

        runBlocking {
            vedtaksvurderingService.underkjennVedtak(
                uuid,
                bruker,
                UnderkjennVedtakClientRequest("kommentar", "begrunnelse")
            )
        }
        val underkjentVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertEquals(VedtakStatus.RETURNERT, underkjentVedtak?.vedtakStatus)

        runBlocking { vedtaksvurderingService.fattVedtak(uuid, bruker) }

        runBlocking { vedtaksvurderingService.attesterVedtak(uuid, bruker) }
        val attestertVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertNotNull(attestertVedtak?.attestant)
        Assertions.assertNotNull(attestertVedtak?.datoattestert)
        Assertions.assertNotNull(attestertVedtak?.virkningsDato)
        Assertions.assertEquals(VedtakStatus.ATTESTERT, attestertVedtak?.vedtakStatus)

        lagreIverksattVedtak(uuid)
        val iverksattVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertEquals(VedtakStatus.IVERKSATT, iverksattVedtak?.vedtakStatus)
    }

    private fun settOpp(uuid: UUID) {
        val beregningDTO = BeregningDTO(
            UUID.randomUUID(),
            uuid,
            Beregningstype.BP,
            listOf(),
            Tidspunkt.now().toLocalDatetimeUTC().toNorskTidspunkt(),
            Metadata(1L, 1L)
        )
        coEvery { beregning.hentBeregning(uuid, any()) } returns beregningDTO
        coEvery { behandling.hentBehandling(uuid, any()) } returns DetaljertBehandling(
            uuid,
            sakId,
            SakType.BARNEPENSJON,
            Tidspunkt.now().toLocalDatetimeUTC(),
            Tidspunkt.now().toLocalDatetimeUTC(),
            Tidspunkt.now().toLocalDatetimeUTC(),
            null,
            "1231245",
            null,
            null,
            null,
            null,
            BehandlingStatus.OPPRETTET,
            BehandlingType.FØRSTEGANGSBEHANDLING,
            null,
            null
        )
        coEvery { behandling.hentSak(any(), any()) } returns mockk<Sak>().apply {
            every { id } returns 1L
            every { ident } returns "ident"
            every { sakType } returns SakType.BARNEPENSJON
        }
        coEvery { vilkaarsvurdering.hentVilkaarsvurdering(uuid, any()) } returns VilkaarsvurderingTestData
            .oppfylt.copy(
                behandlingId = uuid
            )
        coEvery { behandling.fattVedtak(uuid, any(), any()) } returns true
        coEvery { behandling.attester(uuid, any(), any()) } returns true
        coEvery { behandling.underkjenn(uuid, any(), any()) } returns true
    }
}