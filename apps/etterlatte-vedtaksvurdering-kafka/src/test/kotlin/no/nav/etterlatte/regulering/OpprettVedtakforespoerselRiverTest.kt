package no.nav.etterlatte.regulering

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.funksjonsbrytere.DummyFeatureToggleService
import no.nav.etterlatte.klienter.BrevKlient
import no.nav.etterlatte.klienter.UtbetalingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.omregning.OmregningData
import no.nav.etterlatte.omregning.OmregningHendelseType
import no.nav.etterlatte.omregning.UtbetalingVerifikasjon
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.etterlatte.vedtaksvurdering.VedtakService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import vedtakDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class OpprettVedtakforespoerselRiverTest {
    private val fraDato = LocalDate.of(2023, 5, 1)
    private val sakId = randomSakId()

    private val vedtakServiceMock = mockk<VedtakService>(relaxed = true)
    private val utbetalingKlientMock = mockk<UtbetalingKlient>(relaxed = true)
    private val brevKlientMock = mockk<BrevKlient>(relaxed = true)
    private val featureToggleService = DummyFeatureToggleService()

    private fun genererOpprettVedtakforespoersel(
        behandlingId: UUID,
        revurderingaarsak: Revurderingaarsak = Revurderingaarsak.REGULERING,
        utbetalingVerifikasjon: UtbetalingVerifikasjon = UtbetalingVerifikasjon.INGEN,
        forrigeBehandlingId: UUID = UUID.randomUUID(),
    ) = JsonMessage.newMessage(
        mapOf(
            OmregningHendelseType.BEREGNA.lagParMedEventNameKey(),
            HENDELSE_DATA_KEY to
                OmregningData(
                    "kjoering",
                    sakId = sakId,
                    fradato = fraDato,
                    revurderingaarsak = revurderingaarsak,
                    behandlingId = behandlingId,
                    forrigeBehandlingId = forrigeBehandlingId,
                    utbetalingVerifikasjon = utbetalingVerifikasjon,
                ).toPacket(),
        ),
    )

    @BeforeEach
    fun setUp() {
        val vedtakDto =
            vedtakDto(
                behandlingId = UUID.randomUUID(),
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                virk = YearMonth.of(fraDato.year, fraDato.month),
            )
        every { vedtakServiceMock.hentVedtak(any()) } returns vedtakDto
        every { vedtakServiceMock.opprettVedtakOgFatt(any(), any()) } returns
            VedtakOgRapid(
                vedtakDto,
                fattetRapidInfo(vedtakDto),
                null,
            )
        every { vedtakServiceMock.opprettVedtakFattOgAttester(any(), any()) } returns
            VedtakOgRapid(
                vedtakDto,
                fattetRapidInfo(vedtakDto),
                attestertRapidInfo(vedtakDto),
            )
        every { vedtakServiceMock.attesterVedtak(any(), any()) } returns
            VedtakOgRapid(
                vedtakDto,
                attestertRapidInfo(vedtakDto),
                null,
            )
    }

    @AfterEach
    fun tearDown() {
        verify(atLeast = 0) { vedtakServiceMock.hentInnvilgedePerioder(any()) }
        confirmVerified(vedtakServiceMock, utbetalingKlientMock, brevKlientMock)
        clearAllMocks()
    }

    @Test
    fun `skal baade opprette vedtak uten brev og fatte det samt attestere`() {
        val behandlingId = UUID.randomUUID()
        val forrigeBehandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId = behandlingId,
                forrigeBehandlingId = forrigeBehandlingId,
            )
        val vedtakDto = vedtakDto(virk = YearMonth.of(fraDato.year, fraDato.month))
        every { vedtakServiceMock.opprettVedtakFattOgAttester(any(), any()) } returns
            VedtakOgRapid(
                vedtakDto,
                fattetRapidInfo(vedtakDto),
                attestertRapidInfo(vedtakDto),
            )
        every { vedtakServiceMock.hentInnvilgedePerioder(forrigeBehandlingId) } returns
            listOf(
                InnvilgetPeriodeDto(Periode(YearMonth.of(2024, 1), null), emptyList()),
            )
        every { vedtakServiceMock.hentInnvilgedePerioder(behandlingId) } returns
            listOf(
                InnvilgetPeriodeDto(Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 4)), emptyList()),
                InnvilgetPeriodeDto(Periode(YearMonth.of(2024, 5), null), emptyList()),
            )

        val inspector = TestRapid().apply { river() }

        with(inspector.apply { sendTestMessage(melding.toJson()) }.inspektør) {
            verify { vedtakServiceMock.opprettVedtakFattOgAttester(sakId, behandlingId) }
            verify { vedtakServiceMock.hentVedtak(forrigeBehandlingId) }

            size shouldBe 2
            field(0, EVENT_NAME_KEY).asText() shouldBe VedtakKafkaHendelseHendelseType.FATTET.lagEventnameForType()
            field(
                1,
                EVENT_NAME_KEY,
            ).asText() shouldBe VedtakKafkaHendelseHendelseType.ATTESTERT.lagEventnameForType()
            (0..1)
                .forEach { index ->
                    deserialize<VedtakDto>(field(index, "vedtak").toJson())
                        .shouldBeEqualToIgnoringFields(vedtakDto, VedtakDto::innhold)
                    val innvilgedePerioderFoer: List<Periode> =
                        deserialize(field(index, ReguleringEvents.INNVILGEDE_PERIODER_FOER).toJson())
                    val innvilgedePerioderEtter: List<Periode> =
                        deserialize(field(index, ReguleringEvents.INNVILGEDE_PERIODER_ETTER).toJson())
                    innvilgedePerioderFoer shouldBeEqual listOf(Periode(YearMonth.of(2024, 1), null))
                    innvilgedePerioderEtter shouldBeEqual
                        listOf(
                            Periode(YearMonth.of(2024, 1), YearMonth.of(2024, 4)),
                            Periode(YearMonth.of(2024, 5), null),
                        )
                }
        }
    }

    @Test
    fun `skal opprette vedtak og bare fatte hvis feature toggle for stopp er paa`() {
        val behandlingId = UUID.randomUUID()
        val forrigeBehandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId = behandlingId,
                forrigeBehandlingId = forrigeBehandlingId,
            )
        featureToggleService.settBryter(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, true)

        val inspector = TestRapid().apply { river() }
        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { vedtakServiceMock.hentVedtak(forrigeBehandlingId) }
    }

    @Test
    fun `skal baade opprette vedtak og fatte det samt attestere hvis feature toggle for stopp er av`() {
        val behandlingId = UUID.randomUUID()
        val forrigeBehandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId = behandlingId,
                forrigeBehandlingId = forrigeBehandlingId,
            )
        featureToggleService.settBryter(ReguleringFeatureToggle.SkalStoppeEtterFattetVedtak, false)

        val inspector = TestRapid().apply { river() }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakFattOgAttester(sakId, behandlingId) }
        verify { vedtakServiceMock.hentVedtak(forrigeBehandlingId) }
    }

    @Test
    fun `skal opprette vedtak, simulere og attestere ved uendret utbetaling`() {
        val behandlingId = UUID.randomUUID()
        val forrigeBehandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId,
                Revurderingaarsak.OMREGNING,
                UtbetalingVerifikasjon.SIMULERING_AVBRYT_ETTERBETALING_ELLER_TILBAKEKREVING,
                forrigeBehandlingId,
            )
        every { utbetalingKlientMock.simuler(any()) } returns
            mockk {
                every { etterbetaling } returns
                    listOf(
                        mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(100.00) },
                        mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(-100.00) },
                    )
                every { tilbakekreving } returns
                    listOf(
                        mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(100.00) },
                        mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(-100.00) },
                    )
            }

        val inspector = TestRapid().apply { river() }
        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
        verify { vedtakServiceMock.hentVedtak(forrigeBehandlingId) }
        verify { utbetalingKlientMock.simuler(behandlingId) }
    }

    @Test
    fun `skal opprette vedtak, simulere og feile fordi det finnes etterbetaling`() {
        val behandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId,
                Revurderingaarsak.OMREGNING,
                UtbetalingVerifikasjon.SIMULERING_AVBRYT_ETTERBETALING_ELLER_TILBAKEKREVING,
            )
        every { utbetalingKlientMock.simuler(any()) } returns
            mockk {
                every { etterbetaling } returns
                    listOf(
                        mockk(relaxed = true) {
                            every { beloep } returns
                                BigDecimal.valueOf(
                                    100,
                                )
                        },
                    )
                every { tilbakekreving } returns listOf(mockk(relaxed = true))
            }

        val inspector = TestRapid().apply { river() }

        inspector.sendTestMessage(melding.toJson())

        val returnertMelding = inspector.inspektør.message(0)
        returnertMelding.get(EVENT_NAME_KEY).textValue() shouldBe EventNames.FEILA.lagEventnameForType()

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { utbetalingKlientMock.simuler(behandlingId) }
        verify(exactly = 0) { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
    }

    @Test
    fun `skal opprette vedtak, simulere og feile fordi det finnes tilbakekreving`() {
        val behandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId,
                Revurderingaarsak.OMREGNING,
                UtbetalingVerifikasjon.SIMULERING_AVBRYT_ETTERBETALING_ELLER_TILBAKEKREVING,
            )
        every { utbetalingKlientMock.simuler(any()) } returns
            mockk {
                every { etterbetaling } returns listOf(mockk(relaxed = true))
                every { tilbakekreving } returns
                    listOf(mockk(relaxed = true) { every { beloep } returns BigDecimal.valueOf(100) })
            }

        val inspector = TestRapid().apply { river() }
        inspector.sendTestMessage(melding.toJson())

        val returnertMelding = inspector.inspektør.message(0)
        returnertMelding.get(EVENT_NAME_KEY).textValue() shouldBe EventNames.FEILA.lagEventnameForType()

        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { utbetalingKlientMock.simuler(behandlingId) }
        verify(exactly = 0) { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
    }

    @Test
    fun `Aarlig inntektsjustering skal opprette og distribuere brev`() {
        val behandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId,
                revurderingaarsak = Revurderingaarsak.AARLIG_INNTEKTSJUSTERING,
            )

        val inspector = TestRapid().apply { river() }

        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.hentVedtak(any()) }
        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
        verify { brevKlientMock.opprettBrev(behandlingId, sakId) }
        verify { brevKlientMock.genererPdfOgFerdigstillVedtaksbrev(behandlingId, any()) }
    }

    @Test
    fun `Mottatt inntektsjustering skal stoppe ved fattet med brev`() {
        val behandlingId = UUID.randomUUID()
        val forrigeBehandlingId = UUID.randomUUID()
        val melding =
            genererOpprettVedtakforespoersel(
                behandlingId = behandlingId,
                revurderingaarsak = Revurderingaarsak.INNTEKTSENDRING,
                forrigeBehandlingId = forrigeBehandlingId,
            )

        val inspector =
            TestRapid().apply {
                river()
            }
        inspector.sendTestMessage(melding.toJson())

        verify { vedtakServiceMock.hentVedtak(forrigeBehandlingId) }
        verify { vedtakServiceMock.opprettVedtakOgFatt(sakId, behandlingId) }
        verify { brevKlientMock.opprettBrev(behandlingId, sakId) }

        verify(exactly = 0) { vedtakServiceMock.attesterVedtak(sakId, behandlingId) }
        verify(exactly = 0) { brevKlientMock.genererPdfOgFerdigstillVedtaksbrev(behandlingId, any()) }
    }

    private fun TestRapid.river() {
        OpprettVedtakforespoerselRiver(
            this,
            vedtakServiceMock,
            utbetalingKlientMock,
            brevKlientMock,
            featureToggleService,
        )
    }

    private fun attestertRapidInfo(vedtakDto: VedtakDto): RapidInfo =
        RapidInfo(
            VedtakKafkaHendelseHendelseType.ATTESTERT,
            vedtakDto,
            Tidspunkt.now(),
            vedtakDto.behandlingId,
        )

    private fun fattetRapidInfo(vedtakDto: VedtakDto): RapidInfo =
        RapidInfo(
            VedtakKafkaHendelseHendelseType.FATTET,
            vedtakDto,
            Tidspunkt.now(),
            vedtakDto.behandlingId,
        )
}
