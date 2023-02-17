package no.nav.etterlatte.vedtaksvurdering

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Saksbehandler
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatussjekkTest {

    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private val beregning = mockk<BeregningKlient>(relaxed = true)
    private val vilkaarsvurdering = mockk<VilkaarsvurderingKlient>(relaxed = true)
    private val sendToRapid: (String, UUID) -> Unit = mockk(relaxed = true)

    private val saksbehandler = "saksbehandler"
    private val accessToken = "accessToken"
    private val behandlingId = UUID.randomUUID()

    private lateinit var vedtakRepo: VedtaksvurderingRepository
    private val saksbehandlereSecret = mapOf("saksbehandler" to "4808")

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

        vedtakRepo = VedtaksvurderingRepository(dataSource)
    }

    @AfterEach
    fun afterEach() {
        dataSource.connection.prepareStatement(""" TRUNCATE vedtak CASCADE;""").execute()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    private fun opprettVedtak() {
        vedtakRepo.opprettVedtak(
            behandlingsId = behandlingId,
            sakid = 1,
            fnr = "",
            saktype = SakType.BARNEPENSJON,
            behandlingtype = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningsDato = LocalDate.now(),
            beregningsresultat = null,
            vilkaarsresultat = VilkaarsvurderingDto(
                behandlingId = behandlingId,
                vilkaar = listOf(),
                virkningstidspunkt = YearMonth.of(2022, 12),
                resultat = null
            )
        )
    }

    @Test
    fun `skal sjekke og oppdatere behandlingsstatus naar man fatter vedtak`() {
        val behandling = mockk<BehandlingKlient>()
        coEvery { behandling.fattVedtak(any(), any(), any()) } returns true
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid,
            saksbehandlereSecret
        )
        opprettVedtak()

        runBlocking {
            vedtaksvurderingService.fattVedtak(behandlingId, saksbehandler, accessToken)
        }

        coVerifyOrder {
            behandling.fattVedtak(behandlingId, accessToken)
            behandling.fattVedtak(behandlingId, accessToken, any())
        }
    }

    @Test
    fun `skal ikke oppdatere behandlingsstatus hvis vedtak ikke kan fattes`() {
        val behandling = mockk<BehandlingKlient>()
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid,
            saksbehandlereSecret
        )
        coEvery { behandling.fattVedtak(any(), any(), any()) } returns true
        opprettVedtak()
        vedtakRepo.fattVedtak(saksbehandler, saksbehandlereSecret.get(saksbehandler)!!, behandlingId)

        runBlocking {
            assertThrows<KanIkkeEndreFattetVedtak> {
                vedtaksvurderingService.fattVedtak(behandlingId, saksbehandler, accessToken)
            }
        }

        coVerify(exactly = 1) { behandling.fattVedtak(behandlingId, accessToken) }
    }

    @Test
    fun `skal sjekke og oppdatere behandlingsstatus naar man attesterer`() {
        val behandling = mockk<BehandlingKlient>()
        coEvery { behandling.attester(any(), any(), any()) } returns true
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid,
            saksbehandlereSecret
        )
        opprettVedtak()
        vedtakRepo.fattVedtak(saksbehandler, saksbehandlereSecret.get(saksbehandler)!!, behandlingId)

        runBlocking {
            vedtaksvurderingService.attesterVedtak(behandlingId, saksbehandler, accessToken)
        }

        coVerifyOrder {
            behandling.attester(behandlingId, accessToken)
            behandling.attester(behandlingId, accessToken, any())
        }
    }

    @Test
    fun `skal ikke oppdatere behandlingsstatus hvis vedtak ikke kan attesteres`() {
        val behandling = mockk<BehandlingKlient>()
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid,
            saksbehandlereSecret
        )
        coEvery { behandling.attester(any(), any(), any()) } returns true
        opprettVedtak()

        runBlocking {
            assertThrows<VedtakKanIkkeAttesteresFoerDetFattes> {
                vedtaksvurderingService.attesterVedtak(behandlingId, saksbehandler, accessToken)
            }
        }

        coVerify(exactly = 1) { behandling.attester(behandlingId, accessToken, any()) }
    }

    @Test
    fun `skal sjekke og oppdatere behandlingsstatus naar man underkjenner`() {
        val behandling = mockk<BehandlingKlient>()
        coEvery { behandling.underkjenn(any(), any(), any()) } returns true
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid,
            saksbehandlereSecret
        )
        opprettVedtak()
        vedtakRepo.fattVedtak(saksbehandler, saksbehandlereSecret.get(saksbehandler)!!, behandlingId)

        runBlocking {
            vedtaksvurderingService.underkjennVedtak(
                behandlingId,
                accessToken,
                Saksbehandler("saksbehandler"),
                UnderkjennVedtakClientRequest("kommentar", "begrunnelse")
            )
        }

        coVerifyOrder {
            behandling.underkjenn(behandlingId, accessToken)
            behandling.underkjenn(behandlingId, accessToken, any())
        }
    }

    @Test
    fun `skal ikke oppdatere behandlingsstatus hvis vedtak ikke underkjennes`() {
        val behandling = mockk<BehandlingKlient>()
        val vedtaksvurderingService = VedtaksvurderingService(
            vedtakRepo,
            beregning,
            vilkaarsvurdering,
            behandling,
            sendToRapid,
            saksbehandlereSecret
        )
        coEvery { behandling.underkjenn(any(), any()) } returns false
        opprettVedtak()

        runBlocking {
            assertThrows<BehandlingstilstandException> {
                vedtaksvurderingService.underkjennVedtak(
                    behandlingId,
                    accessToken,
                    Saksbehandler(saksbehandler),
                    UnderkjennVedtakClientRequest("kommentar", "begrunnelse")
                )
            }
        }

        coVerify(exactly = 1) { behandling.underkjenn(behandlingId, accessToken, any()) }
    }
}