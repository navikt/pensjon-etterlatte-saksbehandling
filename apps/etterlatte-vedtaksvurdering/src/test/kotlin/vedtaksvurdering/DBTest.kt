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
import no.nav.etterlatte.libs.common.sak.Saksbehandler
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.vilkaarsvurdering.VilkaarsvurderingTestData
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
import java.time.LocalDateTime
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
    private val accessToken = "accessToken"

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

        runBlocking {
            vedtaksvurderingService.underkjennVedtak(
                uuid,
                accessToken,
                Saksbehandler("saksbehandler"),
                UnderkjennVedtakClientRequest("kommentar", "begrunnelse")
            )
        }
        val underkjentVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertEquals(VedtakStatus.RETURNERT, underkjentVedtak?.vedtakStatus)

        runBlocking { vedtaksvurderingService.fattVedtak(uuid, "saksbehandler", accessToken) }

        runBlocking { vedtaksvurderingService.attesterVedtak(uuid, "attestant", accessToken) }
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
            LocalDateTime.now().toTidspunkt(
                norskTidssone
            ),
            Metadata(1L, 1L)
        )
        coEvery { beregning.hentBeregning(uuid, any()) } returns beregningDTO
        coEvery { behandling.hentBehandling(uuid, any()) } returns DetaljertBehandling(
            uuid,
            sakId,
            SakType.BARNEPENSJON,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
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

    @Test
    fun `kan hente vedtak i bolk`() {
        val vedtaksvurderingService = settOppService()
        val behandling1Id = UUID.randomUUID().also { settOpp(it) }
        val behandling2Id = UUID.randomUUID().also { settOpp(it) }
        val behandling3Id = UUID.randomUUID().also { settOpp(it) }

        runBlocking {
            vedtaksvurderingService.opprettEllerOppdaterVedtak(behandling1Id, "access")
            vedtaksvurderingService.opprettEllerOppdaterVedtak(behandling2Id, "access")
        }

        runBlocking {
            vedtaksvurderingService.fattVedtak(behandling1Id, "saksbehandler", accessToken)
            vedtaksvurderingService.fattVedtak(behandling2Id, "saksbehandler", accessToken)
        }

        val vedtakene = vedtaksvurderingService.hentVedtakBolk(listOf(behandling1Id, behandling2Id, behandling3Id))
        Assertions.assertEquals(2, vedtakene.map { it.id }.distinct().size)
    }
}