package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtakStatus
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
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
import vedtaksvurdering.ENHET_1
import vedtaksvurdering.ENHET_2
import vedtaksvurdering.FNR_1
import vedtaksvurdering.SAKSBEHANDLER_1
import vedtaksvurdering.SAKSBEHANDLER_2
import vedtaksvurdering.attestant
import vedtaksvurdering.opprettVedtak
import vedtaksvurdering.saksbehandler
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.util.*
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksvurderingServiceTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var repository: VedtaksvurderingRepository
    private lateinit var dataSource: DataSource

    private val beregningKlientMock = mockk<BeregningKlient>()
    private val vilkaarsvurderingKlientMock = mockk<VilkaarsvurderingKlient>()
    private val behandlingKlientMock = mockk<BehandlingKlient>()
    private val sendToRapidMock = mockk<(String, UUID) -> Unit>(relaxed = true)

    private lateinit var service: VedtaksvurderingService

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        ).also { it.migrate() }

        repository = spyk(VedtaksvurderingRepository(dataSource))
        service = VedtaksvurderingService(
            repository = repository,
            beregningKlient = beregningKlientMock,
            vilkaarsvurderingKlient = vilkaarsvurderingKlientMock,
            behandlingKlient = behandlingKlientMock,
            sendToRapid = sendToRapidMock,
            saksbehandlere = mapOf(
                SAKSBEHANDLER_1 to ENHET_1,
                SAKSBEHANDLER_2 to ENHET_2
            )
        )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @Test
    fun `skal opprette nytt vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregning(any(), any()) } returns mockBeregning(
            virkningstidspunkt,
            behandlingId
        )

        val vedtak = runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }

        vedtak shouldNotBe null
        vedtak.status shouldBe VedtakStatus.OPPRETTET
    }

    @Test
    fun `skal opprette og hente nytt vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregning(any(), any()) } returns mockBeregning(
            virkningstidspunkt,
            behandlingId
        )

        val vedtak = runBlocking {
            service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler)
            service.hentVedtak(behandlingId)
        }

        vedtak shouldNotBe null
        vedtak?.status shouldBe VedtakStatus.OPPRETTET
    }

    // TODO sjekk flere caser rundt opprett

    @Test
    fun `skal ikke kunne oppdatere allerede fattet vedtak`() {
        val behandlingId = randomUUID()

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            repository.fattVedtak(
                behandlingId,
                VedtakFattet(
                    ansvarligSaksbehandler = SAKSBEHANDLER_1,
                    ansvarligEnhet = ENHET_1,
                    tidspunkt = Tidspunkt.now()
                )
            )

            assertThrows<VedtakTilstandException> {
                service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler)
            }
        }
    }

    @Test
    fun `skal oppdatere virkningstidspunkt paa vedtak som ikke er fattet`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt2023 = VIRKNINGSTIDSPUNKT_JAN_2023
        val virkningstidspunkt2024 = VIRKNINGSTIDSPUNKT_JAN_2024

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt2024,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregning(any(), any()) } returns mockBeregning(
            virkningstidspunkt2024,
            behandlingId
        )

        val oppdatertVedtak = runBlocking {
            val nyttVedtak = repository.opprettVedtak(
                opprettVedtak(
                    virkningstidspunkt = virkningstidspunkt2023,
                    behandlingId = behandlingId
                )
            )
            nyttVedtak.virkningstidspunkt shouldBe virkningstidspunkt2023

            service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler)
        }

        oppdatertVedtak.virkningstidspunkt shouldBe virkningstidspunkt2024
    }

    @Test
    fun `skal fatte vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregning(any(), any()) } returns mockBeregning(
            virkningstidspunkt,
            behandlingId
        )

        val fattetVedtak = runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId)
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
        }

        fattetVedtak shouldNotBe null
        with(fattetVedtak.vedtakFattet!!) {
            ansvarligSaksbehandler shouldBe gjeldendeSaksbehandler.ident
            ansvarligEnhet shouldBe ENHET_1
            tidspunkt shouldNotBe null
        }

        coVerify(exactly = 2) { behandlingKlientMock.fattVedtak(any(), any(), any()) }
        verify(exactly = 1) { sendToRapidMock.invoke(any(), any()) }
    }

    @Test
    fun `skal ikke fatte vedtak naar behandling er i ugyldig tilstand`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns false

        runBlocking {
            assertThrows<BehandlingstilstandException> {
                repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
                service.fattVedtak(behandlingId, saksbehandler)
            }
        }
    }

    @Test
    fun `skal ikke fatte vedtak naar vedtak allerede er fattet`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            repository.fattVedtak(
                behandlingId,
                VedtakFattet(
                    ansvarligSaksbehandler = SAKSBEHANDLER_1,
                    ansvarligEnhet = ENHET_1,
                    tidspunkt = Tidspunkt.now()
                )
            )

            assertThrows<VedtakTilstandException> {
                service.fattVedtak(behandlingId, saksbehandler)
            }
        }
    }

    @Test
    fun `skal attestere vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregning(any(), any()) } returns mockBeregning(
            virkningstidspunkt,
            behandlingId
        )

        val attestertVedtak = runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId)
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.attesterVedtak(behandlingId, attestant)
        }

        attestertVedtak shouldNotBe null
        with(attestertVedtak.attestasjon!!) {
            this.attestant shouldBe attestant.ident
            attesterendeEnhet shouldBe ENHET_2
            tidspunkt shouldNotBe null
        }

        coVerify(exactly = 2) { behandlingKlientMock.attester(any(), any(), any()) }
        verify(exactly = 2) { sendToRapidMock.invoke(any(), any()) }
    }

    @Test
    fun `skal ikke attestere vedtak naar behandling er i ugyldig tilstand`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns false

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)

            assertThrows<BehandlingstilstandException> {
                service.attesterVedtak(behandlingId, attestant)
            }
        }
    }

    @Test
    fun `skal ikke attestere vedtak naar vedtak ikke er fattet`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))

            assertThrows<VedtakTilstandException> {
                service.attesterVedtak(behandlingId, saksbehandler)
            }
        }
    }

    @Test
    fun `skal ikke attestere vedtak naar vedtak allerede er attestert`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)
            service.attesterVedtak(behandlingId, attestant)

            assertThrows<VedtakTilstandException> {
                service.attesterVedtak(behandlingId, attestant)
            }
        }
    }

    @Test
    fun `skal sette vedtak til iverksatt`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregning(any(), any()) } returns mockBeregning(
            virkningstidspunkt,
            behandlingId
        )

        val iverksattVedtak = runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId)
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.attesterVedtak(behandlingId, attestant)
            service.iverksattVedtak(behandlingId)
        }

        iverksattVedtak shouldNotBe null
        iverksattVedtak.status shouldBe VedtakStatus.IVERKSATT

        verify(exactly = 1) { sendToRapidMock(match { it.contains(KafkaHendelseType.IVERKSATT.name) }, any()) }
    }

    @Test
    fun `skal ikke sette vedtak til iverksatt naar vedtak ikke er attestert`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))

            assertThrows<VedtakTilstandException> {
                service.iverksattVedtak(behandlingId)
            }
        }
    }

    @Test
    fun `skal ikke sette vedtak til iverksatt naar vedtak allerede er satt til iverksatt`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)
            service.attesterVedtak(behandlingId, attestant)
            service.iverksattVedtak(behandlingId)

            assertThrows<VedtakTilstandException> {
                service.iverksattVedtak(behandlingId)
            }
        }
    }

    @Test
    fun `skal sette vedtak til underkjent`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.underkjenn(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregning(any(), any()) } returns mockBeregning(
            virkningstidspunkt,
            behandlingId
        )

        val underkjentVedtak = runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId)
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.underkjennVedtak(behandlingId, attestant, underkjennVedtakBegrunnelse())
        }

        underkjentVedtak shouldNotBe null
        underkjentVedtak.status shouldBe VedtakStatus.RETURNERT

        verify(exactly = 1) { sendToRapidMock(match { it.contains(KafkaHendelseType.UNDERKJENT.name) }, any()) }
    }

    @Test
    fun `skal ikke underkjenne vedtak naar behandling er i ugyldig tilstand`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.underkjenn(any(), any(), any()) } returns false

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)

            assertThrows<BehandlingstilstandException> {
                service.underkjennVedtak(behandlingId, attestant, underkjennVedtakBegrunnelse())
            }
        }
    }

    @Test
    fun `skal ikke underkjenne vedtak naar vedtak ikke er fattet`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.underkjenn(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))

            assertThrows<VedtakTilstandException> {
                service.underkjennVedtak(behandlingId, saksbehandler, underkjennVedtakBegrunnelse())
            }
        }
    }

    @Test
    fun `skal ikke underkjenne vedtak naar vedtak allerede er attestert`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.underkjenn(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)
            service.attesterVedtak(behandlingId, attestant)

            assertThrows<VedtakTilstandException> {
                service.underkjennVedtak(behandlingId, attestant, underkjennVedtakBegrunnelse())
            }
        }
    }

    private fun underkjennVedtakBegrunnelse() = UnderkjennVedtakDto("Vedtaket er ugyldig", "Annet")

    private fun mockBeregning(virkningstidspunkt: YearMonth, behandlingId_: UUID): BeregningDTO =
        mockk(relaxed = true) {
            every { beregningId } returns randomUUID()
            every { behandlingId } returns behandlingId_
            every { type } returns Beregningstype.BP
            every { beregnetDato } returns Tidspunkt.now()
            every { beregningsperioder } returns listOf(
                Beregningsperiode(
                    datoFOM = virkningstidspunkt,
                    datoTOM = null,
                    utbetaltBeloep = 100,
                    soeskenFlokk = null,
                    grunnbelop = 10000,
                    grunnbelopMnd = 1000,
                    trygdetid = 40
                )
            )
        }

    private fun mockVilkaarsvurdering(): VilkaarsvurderingDto = mockk(relaxed = true) {
        every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
    }

    fun mockBehandling(virk: YearMonth, behandlingId: UUID): DetaljertBehandling = mockk {
        every { id } returns behandlingId
        every { soeker } returns FNR_1
        every { sak } returns 1L
        every { sakType } returns SakType.BARNEPENSJON
        every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
        every { virkningstidspunkt } returns Virkningstidspunkt(
            virk,
            Grunnlagsopplysning.Saksbehandler(SAKSBEHANDLER_1, Instant.now()),
            "enBegrunnelse"
        )
    }

    private companion object {
        val VIRKNINGSTIDSPUNKT_JAN_2023 = YearMonth.of(2023, Month.JANUARY)
        val VIRKNINGSTIDSPUNKT_JAN_2024 = YearMonth.of(2024, Month.JANUARY)
    }
}