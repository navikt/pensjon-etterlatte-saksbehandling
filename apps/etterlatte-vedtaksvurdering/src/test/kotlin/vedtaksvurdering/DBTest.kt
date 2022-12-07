package vedtaksvurdering

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Behandling
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
import java.time.LocalDate
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

    private val uuid = UUID.randomUUID()
    private val sakId = 123L

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

    fun leggtilberegningsresultat() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo, beregning, vilkaarsvurdering, behandling)

        val now = LocalDateTime.now()
        val beregningsperiode = listOf<Beregningsperiode>()
        vedtaksvurderingService.lagreBeregningsresultat(
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, uuid),
            "",
            BeregningsResultat(
                UUID.randomUUID(),
                Beregningstyper.BPGP,
                Endringskode.NY,
                BeregningsResultatType.BEREGNET,
                beregningsperiode,
                now,
                0L
            )
        )
    }

    fun lagreIverksattVedtak() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo, beregning, vilkaarsvurdering, behandling)
        vedtaksvurderingService.lagreIverksattVedtak(uuid)
    }

    @Test
    fun testDB() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo, beregning, vilkaarsvurdering, behandling)

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
        coEvery { vilkaarsvurdering.hentVilkaarsvurdering(any(), any()) } returns VilkaarsvurderingTestData
            .oppfylt.copy(
                behandlingId = uuid
            )
        val vedtaket: Vedtak? = runBlocking {
            vedtaksvurderingService.populerOgHentFellesVedtak(uuid, "access")
        }

        assert(vedtaket?.beregning != null)
        assert(vedtaket?.vilkaarsvurdering != null)
        assert(vedtaket?.sak?.id != null)
        Assertions.assertNotNull(vedtaket?.virk)

        vedtaksvurderingService.fattVedtak(uuid, "saksbehandler")
        val fattetVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertTrue(fattetVedtak?.vedtakFattet!!)
        Assertions.assertEquals(VedtakStatus.FATTET_VEDTAK, fattetVedtak.vedtakStatus)

        vedtaksvurderingService.underkjennVedtak(uuid)
        val underkjentVedtak = vedtaksvurderingService.hentVedtak(uuid)
        Assertions.assertEquals(VedtakStatus.RETURNERT, underkjentVedtak?.vedtakStatus)

        vedtaksvurderingService.fattVedtak(uuid, "saksbehandler")

        vedtaksvurderingService.attesterVedtak(uuid, "attestant")
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

    @Test
    fun `kan lagre og hente alle vedtak`() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo, beregning, vilkaarsvurdering, behandling)

        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val uuid3 = UUID.randomUUID()

        // populerOgHentFellesVedtak(vedtaksvurderingService, uuid1)
        // populerOgHentFellesVedtak(vedtaksvurderingService, uuid2)
        lagreNyttVilkaarsresultat(vedtaksvurderingService, uuid3)

        Assertions.assertEquals(3, vedtaksvurderingService.hentVedtakBolk(listOf(uuid1, uuid2, uuid3)).size)
    }

    @Test
    fun `Skal lagre eller oppdatere virkningstidspunkt`() {
        val vedtakRepo = VedtaksvurderingRepository(dataSource)
        val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo, beregning, vilkaarsvurdering, behandling)
        val behandlingId = UUID.randomUUID()
        val virk = LocalDate.now()

        vedtaksvurderingService.lagreVirkningstidspunkt(behandlingId, virk)
        val vedtak = vedtaksvurderingService.hentVedtak(behandlingId)
        Assertions.assertEquals(virk, vedtak?.virkningsDato)

        vedtaksvurderingService.lagreVirkningstidspunkt(behandlingId, virk.plusDays(1))
        val vedtakOppdatertVirk = vedtaksvurderingService.hentVedtak(behandlingId)
        Assertions.assertEquals(virk.plusDays(1), vedtakOppdatertVirk?.virkningsDato)
    }

    private fun lagreNyttVilkaarsresultat(service: VedtaksvurderingService, behandlingId: UUID) {
        service.lagreVilkaarsresultat(
            SakType.BARNEPENSJON,
            Behandling(BehandlingType.FØRSTEGANGSBEHANDLING, behandlingId),
            "fnr",
            VilkaarsvurderingTestData.oppfylt,
            LocalDate.now()
        )
    }
}