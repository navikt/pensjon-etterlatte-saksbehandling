package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vedtak.KafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
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
import vedtaksvurdering.attestant
import vedtaksvurdering.opprettVedtak
import vedtaksvurdering.saksbehandler
import java.math.BigDecimal
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
            sendToRapid = sendToRapidMock
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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting()
        )

        val vedtak = runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }

        vedtak shouldNotBe null
        vedtak.status shouldBe VedtakStatus.OPPRETTET
    }

    @Test
    fun `skal opprette og hente nytt vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting()
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
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt2024, behandlingId),
            avkorting = mockAvkorting()
        )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting()
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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_2
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting()
        )

        val attestertVedtak = runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId)
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
        }

        attestertVedtak shouldNotBe null
        with(attestertVedtak.attestasjon!!) {
            this.attestant shouldBe attestant.ident
            attesterendeEnhet shouldBe ENHET_2
            tidspunkt shouldNotBe null
        }

        val hendelse = slot<VedtakHendelse>()
        coVerify(exactly = 1) { behandlingKlientMock.attester(any(), any(), null) }
        coVerify(exactly = 1) { behandlingKlientMock.attester(any(), any(), capture(hendelse)) }
        hendelse.captured.kommentar shouldBe KOMMENTAR
        verify(exactly = 2) { sendToRapidMock.invoke(any(), any()) }
    }

    @Test
    fun `attestering av regulering skal ikke foere til brevutsending`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant

        val regulering = DetaljertBehandling(
            id = behandlingId,
            sak = 1L,
            sakType = SakType.BARNEPENSJON,
            behandlingType = BehandlingType.REVURDERING,
            revurderingsaarsak = RevurderingAarsak.REGULERING,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = FNR_1,
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            gyldighetsproeving = null,
            status = BehandlingStatus.VILKAARSVURDERT,
            virkningstidspunkt = null,
            kommerBarnetTilgode = null,
            prosesstype = Prosesstype.MANUELL
        )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns regulering
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting()
        )

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(
                    virkningstidspunkt = virkningstidspunkt,
                    behandlingId = behandlingId
                )
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
        }

        val hendelse = mutableListOf<String>()
        verify(exactly = 2) { sendToRapidMock.invoke(capture(hendelse), any()) }

        val attestertMelding = objectMapper.readTree(hendelse[1])
        attestertMelding.get(EVENT_NAME_KEY).textValue() shouldBe "VEDTAK:ATTESTERT"
        attestertMelding.get(SKAL_SENDE_BREV).isBoolean shouldBe true
        attestertMelding.get(SKAL_SENDE_BREV).booleanValue() shouldBe false
    }

    @Test
    fun `skal ikke attestere vedtak naar behandling er i ugyldig tilstand`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns false
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)

            assertThrows<BehandlingstilstandException> {
                service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
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
                service.attesterVedtak(behandlingId, KOMMENTAR, saksbehandler)
            }
        }
    }

    @Test
    fun `skal ikke attestere vedtak naar vedtak allerede er attestert`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery {
            behandlingKlientMock.hentBehandling(
                any(),
                any()
            )
        } returns mockBehandling(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId)
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId),
            avkorting = mockAvkorting()
        )

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)

            assertThrows<VedtakTilstandException> {
                service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
            }
        }
    }

    @Test
    fun `skal sette vedtak til iverksatt`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting()
        )

        val iverksattVedtak = runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId)
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery {
            behandlingKlientMock.hentBehandling(
                any(),
                any()
            )
        } returns mockBehandling(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId)
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId),
            avkorting = mockAvkorting()
        )

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.underkjenn(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting()
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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )
        coEvery { behandlingKlientMock.fattVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attester(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.underkjenn(any(), any(), any()) } returns true
        coEvery {
            behandlingKlientMock.hentBehandling(
                any(),
                any()
            )
        } returns mockBehandling(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId)
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId),
            avkorting = mockAvkorting()
        )

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)

            assertThrows<VedtakTilstandException> {
                service.underkjennVedtak(behandlingId, attestant, underkjennVedtakBegrunnelse())
            }
        }
    }

    @Test
    fun `tilbakestill vedtak setter status tilbake til returnert`() {
        val behandlingId = randomUUID()
        runBlocking {
            val oppretta = repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
                .let { repository.fattVedtak(behandlingId, VedtakFattet(SAKSBEHANDLER_1, "0001", Tidspunkt.now())) }
            Assertions.assertEquals(oppretta.status, VedtakStatus.FATTET_VEDTAK)
            val tilbakestilt = service.tilbakestillIkkeIverksatteVedtak(behandlingId)
            Assertions.assertEquals(tilbakestilt!!.status, VedtakStatus.RETURNERT)
        }
    }

    @Test
    fun `skal sette utbetaling`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.now()

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId,
            SakType.BARNEPENSJON
        )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns BeregningOgAvkorting(
            beregning = mockBeregning(virkningstidspunkt, behandlingId),
            avkorting = mockAvkorting(virkningstidspunkt)
        )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns Sak(
            SAKSBEHANDLER_1,
            SakType.BARNEPENSJON,
            1L,
            ENHET_1
        )

        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            utbetalingsperioder.size shouldBe 1
            utbetalingsperioder[0].beloep shouldBe BigDecimal(100)
            utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
        }

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns mockBehandling(
            virkningstidspunkt,
            behandlingId,
            SakType.OMSTILLINGSSTOENAD
        )

        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            utbetalingsperioder.size shouldBe 1
            utbetalingsperioder[0].beloep shouldBe BigDecimal(50)
            utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
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

    private fun mockAvkorting(
        virkningstidspunkt: YearMonth = YearMonth.now()
    ): AvkortingDto = mockk(relaxed = true) {
        every { avkortetYtelse } returns listOf(
            AvkortetYtelseDto(
                fom = virkningstidspunkt.atDay(1),
                tom = null,
                ytelseEtterAvkorting = 50,
                avkortingsbeloep = 50
            )
        )
    }

    private fun mockVilkaarsvurdering(): VilkaarsvurderingDto = mockk(relaxed = true) {
        every { resultat?.utfall } returns VilkaarsvurderingUtfall.OPPFYLT
    }

    private fun mockBehandling(
        virk: YearMonth,
        behandlingId: UUID,
        saktype: SakType = SakType.BARNEPENSJON
    ): DetaljertBehandling = mockk {
        every { id } returns behandlingId
        every { soeker } returns FNR_1
        every { sak } returns 1L
        every { sakType } returns saktype
        every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
        every { revurderingsaarsak } returns null
        every { virkningstidspunkt } returns Virkningstidspunkt(
            virk,
            Grunnlagsopplysning.Saksbehandler(SAKSBEHANDLER_1, Tidspunkt.now()),
            "enBegrunnelse"
        )
    }

    private companion object {
        val VIRKNINGSTIDSPUNKT_JAN_2023: YearMonth = YearMonth.of(2023, Month.JANUARY)
        val VIRKNINGSTIDSPUNKT_JAN_2024: YearMonth = YearMonth.of(2024, Month.JANUARY)
        const val KOMMENTAR = "Sendt oppgave til NØP"
    }
}