package no.nav.etterlatte.vedtaksvurdering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.Regelverk
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.GrunnlagOpplysningerDto
import no.nav.etterlatte.libs.common.trygdetid.OpplysningerDifferanse
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.database.transaction
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.vedtaksvurdering.database.DatabaseExtension
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.SamordningsKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.TrygdetidKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class VedtakBehandlingServiceTest(
    private val dataSource: DataSource,
) {
    private lateinit var repository: VedtaksvurderingRepository

    private val beregningKlientMock = mockk<BeregningKlient>()
    private val vilkaarsvurderingKlientMock = mockk<VilkaarsvurderingKlient>()
    private val behandlingKlientMock = mockk<BehandlingKlient>()
    private val samordningsKlientMock = mockk<SamordningsKlient>()
    private val trygdetidKlientMock = mockk<TrygdetidKlient>()
    private val featureToggleServiceMock = mockk<FeatureToggleService>()
    private lateinit var service: VedtakBehandlingService

    @BeforeAll
    fun beforeAll() {
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        repository = spyk(VedtaksvurderingRepository(dataSource))
        service =
            VedtakBehandlingService(
                repository = repository,
                beregningKlient = beregningKlientMock,
                vilkaarsvurderingKlient = vilkaarsvurderingKlientMock,
                behandlingKlient = behandlingKlientMock,
                samordningsKlient = samordningsKlientMock,
                trygdetidKlient = trygdetidKlientMock,
                featureToggleService = featureToggleServiceMock,
            )
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `skal opprette nytt vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val vedtak = runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }

        vedtak shouldNotBe null
        vedtak.status shouldBe VedtakStatus.OPPRETTET
    }

    @Test
    fun `vedtak for opphoer skal legge til opphor fra og med`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns
            mockVilkaarsvurdering(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
            )

        val vedtak = runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }

        vedtak.type shouldBe VedtakType.OPPHOER
        (vedtak.innhold as VedtakInnhold.Behandling).opphoerFraOgMed shouldBe VIRKNINGSTIDSPUNKT_JAN_2023
    }

    @Test
    fun `vedtak skal viderefoere opphoer fra og med hvis finnes`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.REGULERING,
                opphoerFom = YearMonth.of(2023, 3),
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val vedtak = runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }

        (vedtak.innhold as VedtakInnhold.Behandling).opphoerFraOgMed shouldBe YearMonth.of(2023, 3)
    }

    @Test
    fun `vedtak med opphoer fom foer virkningstidspunkt skal kaste exception`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2023, 3)

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.ANNEN,
                opphoerFom = virkningstidspunkt.minusMonths(1),
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        assertThrows<VirkningstidspunktEtterOpphoerException> {
            runBlocking {
                service.opprettEllerOppdaterVedtak(
                    behandlingId,
                    saksbehandler,
                )
            }
        }
    }

    @Test
    fun `skal opprette og hente nytt vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val vedtak =
            runBlocking {
                service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler)
            }

        vedtak shouldNotBe null
        vedtak.status shouldBe VedtakStatus.OPPRETTET
    }

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
                    tidspunkt = Tidspunkt.now(),
                ),
            )

            assertThrows<VedtakTilstandException> {
                service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler)
            }
        }
    }

    @Test
    fun `kan attestere opphørsvedtak på revurderinger av dødsfall`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2022, 8)
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_2,
                null,
                null,
            )
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.DOEDSFALL,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId, type = VedtakType.OPPHOER))
            repository.fattVedtak(
                behandlingId = behandlingId,
                vedtakFattet =
                    VedtakFattet(
                        ansvarligSaksbehandler = saksbehandler.ident,
                        ansvarligEnhet = ENHET_1,
                        tidspunkt = Tidspunkt.now(),
                    ),
            )

            assertDoesNotThrow {
                service.attesterVedtak(behandlingId, "", attestant)
            }
        }
    }

    @Test
    fun `skal oppdatere virkningstidspunkt paa vedtak som ikke er fattet`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt2023 = VIRKNINGSTIDSPUNKT_JAN_2023
        val virkningstidspunkt2024 = VIRKNINGSTIDSPUNKT_JAN_2024

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt2024,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt2024, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val oppdatertVedtak =
            runBlocking {
                val nyttVedtak =
                    repository.opprettVedtak(
                        opprettVedtak(
                            virkningstidspunkt = virkningstidspunkt2023,
                            behandlingId = behandlingId,
                        ),
                    )
                (nyttVedtak.innhold as VedtakInnhold.Behandling).virkningstidspunkt shouldBe virkningstidspunkt2023

                service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler)
            }

        (oppdatertVedtak.innhold as VedtakInnhold.Behandling).virkningstidspunkt shouldBe virkningstidspunkt2024
    }

    @Test
    fun `vedtak for opphoer skal oppdatere opphor fra og med`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2023, 3)
        val endretVirkningstidspunkt = YearMonth.of(2023, 4)

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
            ) andThen
            mockBehandling(
                endretVirkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns
            mockVilkaarsvurdering(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
            )

        runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }
        val vedtak = runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }

        (vedtak.innhold as VedtakInnhold.Behandling).opphoerFraOgMed shouldBe endretVirkningstidspunkt
    }

    @Test
    fun `vedtak som oppdateres fra opphoer til endring skal viderefoere tidligere opphor fra og med`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2023, 3)
        val opphoerFom = YearMonth.of(2023, 4)

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
                opphoerFom = opphoerFom,
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns
            mockVilkaarsvurdering(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
            ) andThen mockVilkaarsvurdering()

        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }
        val vedtak = runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }

        (vedtak.innhold as VedtakInnhold.Behandling).opphoerFraOgMed shouldBe opphoerFom
    }

    @Test
    fun `vedtak som oppdateres fra opphoer til endring med virk fom samme som opphoer fom skal kaste exception`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2023, 3)
        val opphoerFom = YearMonth.of(2023, 3)

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
                opphoerFom = opphoerFom,
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns
            mockVilkaarsvurdering(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
            ) andThen mockVilkaarsvurdering()

        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }
        assertThrows<VirkningstidspunktOgOpphoerFomPaaSammeDatoException> {
            runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }
        }
    }

    @Test
    fun `skal fatte vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val fattetVedtak =
            runBlocking {
                repository.opprettVedtak(
                    opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
                )
                service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            }

        fattetVedtak shouldNotBe null
        with(fattetVedtak.vedtak.vedtakFattet!!) {
            ansvarligSaksbehandler shouldBe gjeldendeSaksbehandler.ident
            ansvarligEnhet shouldBe ENHET_1
            tidspunkt shouldNotBe null
        }

        coVerify(exactly = 1) { behandlingKlientMock.kanFatteVedtak(any(), any()) }
    }

    @Test
    fun `ved fatting av vedtak skal oppdatert beregning, vilkaarsvurdering, trygdetid og behandling brukes`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlientMock.kanFatteVedtak(behandlingId, any()) } returns true
        val virkningstidspunktGammel = VIRKNINGSTIDSPUNKT_JAN_2024
        val virkningstidspunktNy = virkningstidspunktGammel.plusMonths(1)
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                GJENLEVENDE_FOEDSELSNUMMER.value,
                SakType.OMSTILLINGSSTOENAD,
                sakId1,
                ENHET_1,
                null,
                null,
            )

        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virk = virkningstidspunktGammel,
                behandlingId = behandlingId,
                saktype = SakType.OMSTILLINGSSTOENAD,
            ) andThen
            mockBehandling(
                virk = virkningstidspunktNy,
                behandlingId = behandlingId,
                saktype = SakType.OMSTILLINGSSTOENAD,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning =
                    mockBeregning(
                        virkningstidspunkt = virkningstidspunktGammel,
                        behandlingId = behandlingId,
                        beregningstype = Beregningstype.OMS,
                    ),
                avkorting = mockAvkorting(virkningstidspunkt = virkningstidspunktGammel, ytelseEtterAvkorting = 4000),
            ) andThen
            BeregningOgAvkorting(
                beregning =
                    mockBeregning(
                        virkningstidspunkt = virkningstidspunktNy,
                        behandlingId = behandlingId,
                        beregningstype = Beregningstype.OMS,
                    ),
                avkorting = mockAvkorting(virkningstidspunkt = virkningstidspunktNy, ytelseEtterAvkorting = 0),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val opprinneligVedtak =
            runBlocking {
                service.opprettEllerOppdaterVedtak(behandlingId = behandlingId, brukerTokenInfo = saksbehandler)
            }
        val oppdatertVedtak =
            runBlocking {
                service.fattVedtak(behandlingId = behandlingId, brukerTokenInfo = saksbehandler)
            }.vedtak

        with(opprinneligVedtak.innhold as VedtakInnhold.Behandling) {
            Assertions.assertEquals(virkningstidspunktGammel, virkningstidspunkt)
            Assertions.assertEquals(utbetalingsperioder[0].beloep, BigDecimal("4000"))
        }
        with(oppdatertVedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto) {
            Assertions.assertEquals(virkningstidspunktNy, virkningstidspunkt)
            Assertions.assertEquals(utbetalingsperioder[0].beloep, BigDecimal("0"))
        }

        coVerify(exactly = 1) { behandlingKlientMock.kanFatteVedtak(any(), any()) }
    }

    @Test
    fun `skal ikke fatte vedtak naar behandling er i ugyldig tilstand`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns false

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

        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            repository.fattVedtak(
                behandlingId,
                VedtakFattet(
                    ansvarligSaksbehandler = SAKSBEHANDLER_1,
                    ansvarligEnhet = ENHET_1,
                    tidspunkt = Tidspunkt.now(),
                ),
            )

            assertThrows<VedtakTilstandException> {
                service.fattVedtak(behandlingId, saksbehandler)
            }
        }
    }

    @Test
    fun `skal rulle tilbake vedtak som blir fattet hvis attesteringsoppgave feiler`() {
        val behandlingId = randomUUID()
        val sakId = sakId1
        val virkningstidspunkt = YearMonth.of(2022, Month.AUGUST)
        val gjeldendeSaksbehandler = saksbehandler

        coEvery { behandlingKlientMock.kanFatteVedtak(behandlingId, any()) } returns true
        coEvery {
            behandlingKlientMock.fattVedtakBehandling(any(), any())
        } throws RuntimeException("Å nei")
        coEvery { behandlingKlientMock.hentBehandling(behandlingId, any()) } returns
            mockBehandling(
                virk = virkningstidspunkt,
                behandlingId = behandlingId,
                sakId = sakId,
            )
        coEvery { behandlingKlientMock.hentSak(sakId, any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId,
                ENHET_2,
                null,
                null,
            )
        coEvery {
            vilkaarsvurderingKlientMock.hentVilkaarsvurdering(
                behandlingId,
                any(),
            )
        } returns mockVilkaarsvurdering()
        coEvery {
            beregningKlientMock.hentBeregningOgAvkorting(
                behandlingId,
                any(),
                SakType.BARNEPENSJON,
            )
        } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
                avkorting = mockAvkorting(),
            )

        val opprettetVedtak =
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId, sakId = sakId),
            )

        assertThrows<Exception> {
            runBlocking {
                service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            }
        }
        val vedtakEtterFeiletFatting = repository.hentVedtak(behandlingId)
        Assertions.assertEquals(opprettetVedtak, vedtakEtterFeiletFatting)

        // Sjekker også at den respekterer opprinnelig status på vedtak:
        val returnertVedtak = opprettetVedtak.copy(status = VedtakStatus.RETURNERT)
        dataSource.transaction { tx ->
            queryOf(
                "UPDATE vedtak SET vedtakstatus = :vedtakstatus WHERE behandlingid = :behandlingId",
                mapOf(
                    "vedtakstatus" to returnertVedtak.status.name,
                    "behandlingId" to returnertVedtak.behandlingId,
                ),
            ).let { query -> tx.run(query.asUpdate) }
        }

        assertThrows<Exception> {
            runBlocking {
                service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            }
        }
        val returnertVedtakEtterFeiletFatting = repository.hentVedtak(behandlingId)
        Assertions.assertEquals(returnertVedtak, returnertVedtakEtterFeiletFatting)
    }

    @Test
    fun `skal attestere vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_2,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val attestertVedtak =
            runBlocking {
                repository.opprettVedtak(
                    opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
                )
                service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
                service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
            }

        attestertVedtak shouldNotBe null
        with(attestertVedtak.vedtak.attestasjon!!) {
            this.attestant shouldBe attestant.ident
            attesterendeEnhet shouldBe ENHET_2
            tidspunkt shouldNotBe null
        }

        val hendelse = slot<VedtakEndringDTO>()
        coVerify(exactly = 1) { behandlingKlientMock.kanAttestereVedtak(any(), any(), null) }
        coVerify(exactly = 1) { behandlingKlientMock.attesterVedtak(any(), capture(hendelse)) }
        hendelse.captured.vedtakHendelse.kommentar shouldBe KOMMENTAR
        hendelse.captured.sakIdOgReferanse.referanse shouldBe behandlingId.toString()
    }

    @Test
    fun `attestering av egen sak kaster feil`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_2,
                null,
                null,
            )
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)

            shouldThrow<UgyldigAttestantException> {
                service.attesterVedtak(behandlingId, KOMMENTAR, gjeldendeSaksbehandler)
            }
        }

        coVerify {
            behandlingKlientMock.kanFatteVedtak(any(), any()) // sjekke status behandling
            behandlingKlientMock.fattVedtakBehandling(any(), any())
            behandlingKlientMock.kanAttestereVedtak(any(), any(), null) // sjekke status behandling
        }
    }

    @Test
    fun `attestering av regulering skal ikke foere til brevutsending`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant

        val regulering =
            DetaljertBehandling(
                id = behandlingId,
                sak = sakId1,
                sakType = SakType.BARNEPENSJON,
                behandlingType = BehandlingType.REVURDERING,
                revurderingsaarsak = Revurderingaarsak.REGULERING,
                soeker = SOEKER_FOEDSELSNUMMER.value,
                status = BehandlingStatus.VILKAARSVURDERT,
                virkningstidspunkt =
                    Virkningstidspunkt(
                        dato = virkningstidspunkt.withMonth(Month.MAY.value),
                        kilde = Grunnlagsopplysning.automatiskSaksbehandler,
                        begrunnelse = "",
                        kravdato = null,
                    ),
                boddEllerArbeidetUtlandet = null,
                utlandstilknytning = null,
                prosesstype = Prosesstype.MANUELL,
                revurderingInfo = null,
                vedtaksloesning = Vedtaksloesning.GJENNY,
                sendeBrev = false,
                opphoerFraOgMed = null,
                relatertBehandlingId = null,
                tidligereFamiliepleier = null,
                opprinnelse = BehandlingOpprinnelse.AUTOMATISK_JOBB,
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns regulering
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val attestering =
            runBlocking {
                repository.opprettVedtak(
                    opprettVedtak(
                        virkningstidspunkt = virkningstidspunkt,
                        behandlingId = behandlingId,
                    ),
                )
                service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
                service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
            }

        val hendelse = attestering.rapidInfo1

        Assertions.assertEquals(hendelse.vedtakhendelse, VedtakKafkaHendelseHendelseType.ATTESTERT)
        Assertions.assertEquals(false, hendelse.extraParams[SKAL_SENDE_BREV])
    }

    @Test
    fun `skal ikke attestere vedtak naar behandling er i ugyldig tilstand`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns false
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                YearMonth.now(),
                behandlingId,
            )
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning =
                    mockk<BeregningDTO>(relaxed = true).also {
                        every {
                            it.beregningsperioder
                        } returns
                            listOf(
                                Beregningsperiode(
                                    datoFOM = YearMonth.of(2023, Month.JANUARY),
                                    datoTOM = null,
                                    utbetaltBeloep = 100,
                                    grunnbelopMnd = 0,
                                    grunnbelop = 0,
                                    trygdetid = 40,
                                ),
                            )
                    },
                avkorting = null,
            )

        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

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

        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true

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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery {
            behandlingKlientMock.hentBehandling(
                any(),
                any(),
            )
        } returns mockBehandling(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId)
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

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
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.iverksett(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val iverksattVedtak =
            runBlocking {
                repository.opprettVedtak(
                    opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
                )
                service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
                service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
                service.iverksattVedtak(behandlingId, attestant)
            }

        iverksattVedtak shouldNotBe null
        iverksattVedtak.vedtak.status shouldBe VedtakStatus.IVERKSATT

        Assertions.assertEquals(VedtakKafkaHendelseHendelseType.IVERKSATT, iverksattVedtak.rapidInfo1.vedtakhendelse)
    }

    @Test
    fun `skal rulle tilbake vedtak ved iverksatt dersom behandling feiler`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )

        coEvery { behandlingKlientMock.iverksett(any(), any(), any()) } throws RuntimeException("Behandling feilet")
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
        }

        assertThrows<RuntimeException> {
            runBlocking {
                service.iverksattVedtak(behandlingId, attestant)
            }
        }
        val ikkeIverksattVedtak = repository.hentVedtak(behandlingId)!!
        ikkeIverksattVedtak shouldNotBe null
        ikkeIverksattVedtak.status shouldNotBe VedtakStatus.IVERKSATT
        ikkeIverksattVedtak.status shouldBe VedtakStatus.ATTESTERT
    }

    @Test
    fun `skal rulle tilbake vedtak ved iverksatt dersom behandling returnerer false`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        val attestant = attestant
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )

        coEvery { behandlingKlientMock.iverksett(any(), any(), any()) } returns false
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
            )
            service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
        }

        assertThrows<BehandlingIverksettelseException> {
            runBlocking {
                service.iverksattVedtak(behandlingId, attestant)
            }
        }
        val ikkeIverksattVedtak = repository.hentVedtak(behandlingId)!!
        ikkeIverksattVedtak shouldNotBe null
        ikkeIverksattVedtak.status shouldNotBe VedtakStatus.IVERKSATT
        ikkeIverksattVedtak.status shouldBe VedtakStatus.ATTESTERT
    }

    @Test
    fun `skal ikke sette vedtak til iverksatt naar vedtak ikke er attestert`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))

            assertThrows<VedtakTilstandException> {
                service.iverksattVedtak(behandlingId, attestant)
            }
        }
    }

    @Test
    fun `skal ikke sette vedtak til iverksatt naar vedtak allerede er satt til iverksatt`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.iverksett(any(), any(), any()) } returns true
        coEvery {
            behandlingKlientMock.hentBehandling(
                any(),
                any(),
            )
        } returns mockBehandling(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId)
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId))
            service.fattVedtak(behandlingId, saksbehandler)
            service.attesterVedtak(behandlingId, KOMMENTAR, attestant)
            service.iverksattVedtak(behandlingId, attestant)

            assertThrows<VedtakTilstandException> {
                service.iverksattVedtak(behandlingId, attestant)
            }
        }
    }

    @Test
    fun `skal sette vedtak til underkjent`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = VIRKNINGSTIDSPUNKT_JAN_2023
        val gjeldendeSaksbehandler = saksbehandler
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanUnderkjenneVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
            )
        coEvery { behandlingKlientMock.underkjennVedtak(any(), any()) } returns true
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val underkjentVedtak =
            runBlocking {
                repository.opprettVedtak(
                    opprettVedtak(virkningstidspunkt = virkningstidspunkt, behandlingId = behandlingId),
                )
                service.fattVedtak(behandlingId, gjeldendeSaksbehandler)
                service.underkjennVedtak(behandlingId, attestant, underkjennVedtakBegrunnelse())
            }
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        underkjentVedtak shouldNotBe null
        underkjentVedtak.vedtak.status shouldBe VedtakStatus.RETURNERT

        Assertions.assertEquals(VedtakKafkaHendelseHendelseType.UNDERKJENT, underkjentVedtak.rapidInfo1.vedtakhendelse)
    }

    @Test
    fun `skal ikke underkjenne vedtak naar behandling er i ugyldig tilstand`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanUnderkjenneVedtak(any(), any(), any()) } returns false
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                YearMonth.now(),
                behandlingId,
            )
        coEvery {
            beregningKlientMock.hentBeregningOgAvkorting(
                any(),
                any(),
                any(),
            )
        } returns
            BeregningOgAvkorting(
                beregning =
                    mockk<BeregningDTO>(relaxed = true).also {
                        every {
                            it.beregningsperioder
                        } returns
                            listOf(
                                Beregningsperiode(
                                    datoFOM = YearMonth.of(2023, Month.JANUARY),
                                    datoTOM = null,
                                    utbetaltBeloep = 100,
                                    grunnbelopMnd = 0,
                                    grunnbelop = 0,
                                    trygdetid = 40,
                                ),
                            )
                    },
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

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

        coEvery { behandlingKlientMock.kanUnderkjenneVedtak(any(), any(), any()) } returns true

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
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.kanFatteVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanAttestereVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.attesterVedtak(any(), any()) } returns true
        coEvery { behandlingKlientMock.kanUnderkjenneVedtak(any(), any(), any()) } returns true
        coEvery { behandlingKlientMock.fattVedtakBehandling(any(), any()) } returns true
        coEvery {
            behandlingKlientMock.hentBehandling(
                any(),
                any(),
            )
        } returns mockBehandling(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId)
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(VIRKNINGSTIDSPUNKT_JAN_2023, behandlingId),
                avkorting = null,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

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
            val oppretta =
                repository
                    .opprettVedtak(opprettVedtak(behandlingId = behandlingId))
                    .let {
                        repository.fattVedtak(
                            behandlingId,
                            VedtakFattet(SAKSBEHANDLER_1, ENHET_1, Tidspunkt.now()),
                        )
                    }
            Assertions.assertEquals(oppretta.status, VedtakStatus.FATTET_VEDTAK)
            val tilbakestilt = service.tilbakestillIkkeIverksatteVedtak(behandlingId)
            Assertions.assertEquals(tilbakestilt!!.status, VedtakStatus.RETURNERT)
        }
    }

    @Test
    fun `skal sette utbetaling`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.now()

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                SakType.BARNEPENSJON,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(virkningstidspunkt),
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )

        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            val innhold = innhold as VedtakInnhold.Behandling
            innhold.utbetalingsperioder.size shouldBe 1
            innhold.utbetalingsperioder[0].beloep shouldBe BigDecimal(100)
            innhold.utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
        }

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                SakType.OMSTILLINGSSTOENAD,
            )

        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            val innhold = innhold as VedtakInnhold.Behandling
            innhold.utbetalingsperioder.size shouldBe 1
            innhold.utbetalingsperioder[0].beloep shouldBe BigDecimal(50)
            innhold.utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
        }
    }

    @Test
    fun `skal sette utbetaling hvis opphoer`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.now()

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                SakType.BARNEPENSJON,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns
            mockVilkaarsvurdering(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )

        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            val innhold = innhold as VedtakInnhold.Behandling
            innhold.utbetalingsperioder.size shouldBe 1
            innhold.utbetalingsperioder[0].beloep shouldBe null
            innhold.utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
        }
    }

    @Test
    fun `skal sette utbetaling hvis opphoer etter oppdatert vedtak`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.now()

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                SakType.BARNEPENSJON,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns
            mockVilkaarsvurdering(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
            )

        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }
        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            val innhold = innhold as VedtakInnhold.Behandling
            innhold.utbetalingsperioder.size shouldBe 1
            innhold.utbetalingsperioder[0].beloep shouldBe null
            innhold.utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
        }
    }

    @Test
    fun `skal sette utbetaling hvis nytt opphoer med eksisterende opphoer fra og med`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2024, 3)

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                SakType.BARNEPENSJON,
                revurderingAarsak = Revurderingaarsak.ALDERSOVERGANG,
                opphoerFom = YearMonth.of(2024, 4),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns
            mockVilkaarsvurdering(
                utfall = VilkaarsvurderingUtfall.IKKE_OPPFYLT,
            )

        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )

        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            val innhold = innhold as VedtakInnhold.Behandling
            innhold.utbetalingsperioder.size shouldBe 1
            innhold.utbetalingsperioder[0].beloep shouldBe null
            innhold.utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
        }
    }

    @Test
    fun `skal sette utbetaling endring med eksisterende opphoer fra og med`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2024, 3)
        val opphoer = YearMonth.of(2024, 4)

        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                SakType.BARNEPENSJON,
                revurderingAarsak = Revurderingaarsak.REGULERING,
                opphoerFom = opphoer,
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning = mockBeregning(virkningstidspunkt, behandlingId),
                avkorting = mockAvkorting(virkningstidspunkt),
            )
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.BARNEPENSJON,
                sakId1,
                ENHET_1,
                null,
                null,
            )

        with(runBlocking { service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler) }) {
            val innhold = innhold as VedtakInnhold.Behandling
            innhold.utbetalingsperioder.size shouldBe 2
            innhold.utbetalingsperioder[0].beloep shouldBe BigDecimal(100)
            innhold.utbetalingsperioder[0].periode.fom shouldBe virkningstidspunkt
            innhold.utbetalingsperioder[0].periode.tom shouldBe virkningstidspunkt

            innhold.utbetalingsperioder[1].beloep shouldBe null
            innhold.utbetalingsperioder[1].periode.fom shouldBe opphoer
            innhold.utbetalingsperioder[1].periode.tom shouldBe null
        }
    }

    @Test
    fun `skal ikke sette vedtak til til_samordning pga ugyldig vedtaksstatus`() {
        val behandlingId = randomUUID()

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId, status = VedtakStatus.FATTET_VEDTAK))

            assertThrows<VedtakTilstandException> {
                service.tilSamordningVedtak(behandlingId, attestant)
            }

            coVerify { behandlingKlientMock wasNot called }
        }
    }

    @Test
    fun `skal sette vedtak til til_samordning`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.tilSamordning(behandlingId, attestant, any()) } returns true

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(
                    behandlingId = behandlingId,
                    status = VedtakStatus.ATTESTERT,
                    soeker = Folkeregisteridentifikator.of("08815997000"),
                ),
            )

            val oppdatertVedtak = service.tilSamordningVedtak(behandlingId, attestant)

            oppdatertVedtak.vedtak.status shouldBe VedtakStatus.TIL_SAMORDNING

            coVerify(exactly = 1) { behandlingKlientMock.tilSamordning(behandlingId, attestant, any()) }
            coVerify(exactly = 0) {
                samordningsKlientMock.samordneVedtak(
                    any(),
                    EtterbetalingResultat(false, false),
                    attestant,
                )
            }
        }
    }

    @Test
    fun `skal rulle tilbake hvis sette vedtak til til_samordning feiler`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.tilSamordning(behandlingId, attestant, any()) } returns false

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(
                    behandlingId = behandlingId,
                    status = VedtakStatus.ATTESTERT,
                    soeker = Folkeregisteridentifikator.of("08815997000"),
                ),
            )

            assertThrows<VedtakTilSamordningException> {
                service.tilSamordningVedtak(behandlingId, attestant)
            }
        }

        val ikkeTilSamordningVedtak = repository.hentVedtak(behandlingId)!!
        ikkeTilSamordningVedtak shouldNotBe null
        ikkeTilSamordningVedtak.status shouldNotBe VedtakStatus.TIL_SAMORDNING
        ikkeTilSamordningVedtak.status shouldBe VedtakStatus.ATTESTERT
    }

    @Test
    fun `skal kalle SAM for aa samordne, ikke oppdatere vedtaksstatus`() {
        mockkStatic(Vedtak::erVedtakMedEtterbetaling)
        val grunnbeloep = mockk<Grunnbeloep> { every { grunnbeloep } returns 150000 }
        val etterbetalingResultat = EtterbetalingResultat(erEtterbetaling = true)
        every { any<Vedtak>().erVedtakMedEtterbetaling(repository, grunnbeloep) } returns etterbetalingResultat
        coEvery { beregningKlientMock.hentGrunnbeloep(any()) } returns grunnbeloep

        val behandlingId = randomUUID()

        coEvery { samordningsKlientMock.samordneVedtak(any(), any(), attestant) } returns true

        runBlocking {
            repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId, status = VedtakStatus.TIL_SAMORDNING))

            service.samordne(behandlingId, attestant) shouldBe true

            coVerify(exactly = 1) { samordningsKlientMock.samordneVedtak(any(), etterbetalingResultat, attestant) }
            coVerify(exactly = 0) { behandlingKlientMock.samordnet(behandlingId, any(), any()) }
        }

        verify { any<Vedtak>().erVedtakMedEtterbetaling(repository, grunnbeloep) }
    }

    @Test
    fun `skal ikke kalle SAM for aa samordne hvis REGULERING`() {
        val behandlingId = randomUUID()

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(
                    behandlingId = behandlingId,
                    status = VedtakStatus.TIL_SAMORDNING,
                    revurderingAarsak = Revurderingaarsak.REGULERING,
                ),
            )

            service.samordne(behandlingId, attestant) shouldBe false

            coVerify(exactly = 0) {
                samordningsKlientMock.samordneVedtak(
                    vedtak = any(),
                    etterbetaling = any(),
                    brukerTokenInfo = attestant,
                )
            }
        }
    }

    @Test
    fun `skal sette vedtak til samordnet`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.tilSamordning(behandlingId, attestant, any()) } returns true
        coEvery { behandlingKlientMock.samordnet(any(), any(), any()) } returns true
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(
                    behandlingId = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    type = VedtakType.ENDRING,
                    status = VedtakStatus.ATTESTERT,
                    revurderingAarsak = Revurderingaarsak.INNTEKTSENDRING,
                ),
            )
            service.tilSamordningVedtak(behandlingId, attestant)
            val oppdatertVedtak = service.samordnetVedtak(behandlingId, attestant)!!

            oppdatertVedtak.vedtak.status shouldBe VedtakStatus.SAMORDNET

            coVerify(exactly = 1) { behandlingKlientMock.tilSamordning(behandlingId, attestant, any()) }
            coVerify(exactly = 1) { behandlingKlientMock.samordnet(behandlingId, any(), any()) }
            coVerify(exactly = 0) { samordningsKlientMock.samordneVedtak(any(), any(), attestant) }
        }
    }

    @Test
    fun `skal rulle vedtak tilbake ved feil under setting av vedtak til samordnet`() {
        val behandlingId = randomUUID()

        coEvery { behandlingKlientMock.tilSamordning(behandlingId, attestant, any()) } returns true
        coEvery { behandlingKlientMock.samordnet(any(), any(), any()) } returns false
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        runBlocking {
            repository.opprettVedtak(
                opprettVedtak(
                    behandlingId = behandlingId,
                    behandlingType = BehandlingType.REVURDERING,
                    type = VedtakType.ENDRING,
                    status = VedtakStatus.ATTESTERT,
                    revurderingAarsak = Revurderingaarsak.INNTEKTSENDRING,
                ),
            )
            service.tilSamordningVedtak(behandlingId, attestant)

            assertThrows<VedtakSamordnetException> {
                service.samordnetVedtak(behandlingId, attestant)
            }
        }

        val ikkeSamordnetVedtak = repository.hentVedtak(behandlingId)!!
        ikkeSamordnetVedtak shouldNotBe null
        ikkeSamordnetVedtak.status shouldNotBe VedtakStatus.SAMORDNET
        ikkeSamordnetVedtak.status shouldBe VedtakStatus.TIL_SAMORDNING
    }

    @Test
    fun `skal ikke sette vedtak til samordnet pga ugyldig vedtaksstatus for oppdatering`() {
        val behandlingId = randomUUID()

        repository.opprettVedtak(opprettVedtak(behandlingId = behandlingId, status = VedtakStatus.ATTESTERT))

        assertThrows<VedtakTilstandException> {
            service.samordnetVedtak(behandlingId, attestant)
        }

        coVerify { behandlingKlientMock wasNot called }
    }

    @Test
    fun `skal opprette nytt vedtak og sette riktig regelverk for perioder`() {
        val behandlingId = randomUUID()
        val virkningstidspunkt = YearMonth.of(2024, Month.APRIL)
        coEvery { behandlingKlientMock.hentSak(any(), any()) } returns
            Sak(
                SAKSBEHANDLER_1,
                SakType.OMSTILLINGSSTOENAD,
                sakId1,
                ENHET_1,
                null,
                null,
            )
        coEvery { behandlingKlientMock.hentBehandling(any(), any()) } returns
            mockBehandling(
                virkningstidspunkt,
                behandlingId,
                saktype = SakType.OMSTILLINGSSTOENAD,
            )
        coEvery { vilkaarsvurderingKlientMock.hentVilkaarsvurdering(any(), any()) } returns mockVilkaarsvurdering()
        coEvery { beregningKlientMock.hentBeregningOgAvkorting(any(), any(), any()) } returns
            BeregningOgAvkorting(
                beregning =
                    mockBeregning(
                        virkningstidspunkt,
                        behandlingId,
                        beregningsperioder =
                            listOf(
                                mockBeregningsperiode(
                                    fom = YearMonth.of(2024, Month.APRIL),
                                    tom = YearMonth.of(2024, Month.APRIL),
                                ),
                                mockBeregningsperiode(
                                    fom = YearMonth.of(2024, Month.MAY),
                                    tom = YearMonth.of(2025, Month.JANUARY),
                                ),
                            ),
                    ),
                avkorting =
                    mockAvkorting(
                        avkortetYtelse =
                            listOf(
                                mockAvkortetYtelse(
                                    fom = YearMonth.of(2024, Month.APRIL),
                                    tom = YearMonth.of(2024, Month.APRIL),
                                ),
                                mockAvkortetYtelse(
                                    fom = YearMonth.of(2024, Month.MAY),
                                    tom = YearMonth.of(2024, Month.DECEMBER),
                                ),
                                mockAvkortetYtelse(fom = YearMonth.of(2025, Month.JANUARY), tom = null),
                            ),
                    ),
            )
        coEvery { trygdetidKlientMock.hentTrygdetid(any(), any()) } returns trygdetidDtoUtenDiff()

        val vedtak =
            runBlocking {
                service.opprettEllerOppdaterVedtak(behandlingId, saksbehandler)
            }

        vedtak shouldNotBe null
        vedtak.status shouldBe VedtakStatus.OPPRETTET
    }

    private fun mockBeregningsperiode(
        fom: YearMonth,
        tom: YearMonth?,
    ): Beregningsperiode =
        Beregningsperiode(
            datoFOM = fom,
            datoTOM = tom,
            utbetaltBeloep = 100,
            soeskenFlokk = null,
            grunnbelop = 10000,
            grunnbelopMnd = 1000,
            trygdetid = 40,
            regelverk = Regelverk.REGELVERK_FOM_JAN_2024,
        )

    private fun mockAvkortetYtelse(
        fom: YearMonth,
        tom: YearMonth?,
    ) = AvkortetYtelseDto(
        fom = fom,
        tom = tom,
        ytelseFoerAvkorting = 100,
        ytelseEtterAvkorting = 50,
        avkortingsbeloep = 50,
        restanse = 0,
        sanksjon = null,
    )

    private fun underkjennVedtakBegrunnelse() = UnderkjennVedtakDto("Vedtaket er ugyldig", "Annet")

    private fun mockBeregning(
        virkningstidspunkt: YearMonth,
        behandlingId: UUID,
        beregningstype: Beregningstype = Beregningstype.BP,
        beregningsperioder: List<Beregningsperiode> =
            listOf(
                Beregningsperiode(
                    datoFOM = virkningstidspunkt,
                    datoTOM = null,
                    utbetaltBeloep = 100,
                    soeskenFlokk = null,
                    grunnbelop = 10000,
                    grunnbelopMnd = 1000,
                    trygdetid = 40,
                ),
            ),
    ): BeregningDTO =
        mockk(relaxed = true) {
            every { beregningId } returns randomUUID()
            every { this@mockk.behandlingId } returns behandlingId
            every { type } returns beregningstype
            every { beregnetDato } returns Tidspunkt.now()
            every { this@mockk.beregningsperioder } returns beregningsperioder
        }

    private fun mockAvkorting(
        virkningstidspunkt: YearMonth = YearMonth.now(),
        ytelseEtterAvkorting: Int = 50,
        avkortetYtelse: List<AvkortetYtelseDto> =
            listOf(
                AvkortetYtelseDto(
                    fom = virkningstidspunkt,
                    tom = null,
                    ytelseFoerAvkorting = 100,
                    ytelseEtterAvkorting = ytelseEtterAvkorting,
                    avkortingsbeloep = 50,
                    restanse = 0,
                    sanksjon = null,
                ),
            ),
    ): AvkortingDto =
        mockk(relaxed = true) {
            every { this@mockk.avkortetYtelse } returns avkortetYtelse
        }

    private fun mockVilkaarsvurdering(utfall: VilkaarsvurderingUtfall = VilkaarsvurderingUtfall.OPPFYLT): VilkaarsvurderingDto =
        mockk(relaxed = true) {
            every { resultat?.utfall } returns utfall
        }

    private fun mockBehandling(
        virk: YearMonth,
        behandlingId: UUID,
        saktype: SakType = SakType.BARNEPENSJON,
        revurderingAarsak: Revurderingaarsak? = null,
        revurderingInfo: RevurderingInfo? = null,
        sakId: SakId = sakId1,
        opphoerFom: YearMonth? = null,
    ): DetaljertBehandling =
        DetaljertBehandling(
            id = behandlingId,
            sak = sakId,
            sakType = saktype,
            soeker = SOEKER_FOEDSELSNUMMER.value,
            status = BehandlingStatus.OPPRETTET,
            behandlingType =
                if (revurderingAarsak == null) {
                    BehandlingType.FØRSTEGANGSBEHANDLING
                } else {
                    BehandlingType.REVURDERING
                },
            virkningstidspunkt =
                Virkningstidspunkt(
                    virk,
                    Grunnlagsopplysning.Saksbehandler(SAKSBEHANDLER_1, Tidspunkt.now()),
                    "enBegrunnelse",
                ),
            boddEllerArbeidetUtlandet = null,
            utlandstilknytning = null,
            revurderingsaarsak = revurderingAarsak,
            revurderingInfo = revurderingInfo,
            prosesstype = Prosesstype.MANUELL,
            vedtaksloesning = Vedtaksloesning.GJENNY,
            sendeBrev = true,
            opphoerFraOgMed = opphoerFom,
            relatertBehandlingId = null,
            tidligereFamiliepleier = null,
            opprinnelse = BehandlingOpprinnelse.UKJENT,
        )

    private companion object {
        val VIRKNINGSTIDSPUNKT_JAN_2023: YearMonth = YearMonth.of(2023, Month.JANUARY)
        val VIRKNINGSTIDSPUNKT_JAN_2024: YearMonth = YearMonth.of(2024, Month.JANUARY)
        const val KOMMENTAR = "Sendt oppgave til NØP"
    }

    private fun trygdetidDtoUtenDiff(): List<TrygdetidDto> {
        val oppdaterteGrunnlagsopplysninger = mockk<GrunnlagOpplysningerDto>()
        val trygdetidDto =
            mockk<TrygdetidDto> {
                every { opplysningerDifferanse } returns
                    OpplysningerDifferanse(false, oppdaterteGrunnlagsopplysninger)
            }
        return listOf(trygdetidDto)
    }
}
