package no.nav.etterlatte.statistikk.service

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.domain.AvkortetYtelse
import no.nav.etterlatte.statistikk.domain.Avkorting
import no.nav.etterlatte.statistikk.domain.AvkortingGrunnlag
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.Beregningstype
import no.nav.etterlatte.statistikk.domain.MaanedStatistikk
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.river.BehandlingHendelse
import no.nav.etterlatte.statistikk.river.BehandlingIntern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.*

class StatistikkServiceTest {

    private val stoenadRepo = mockk<StoenadRepository>()
    private val sakRepo = mockk<SakRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val beregningKlient = mockk<BeregningKlient>()
    private val service = StatistikkService(
        stoenadRepository = stoenadRepo,
        sakRepository = sakRepo,
        behandlingKlient = behandlingKlient,
        beregningKlient = beregningKlient
    )

    @Test
    fun `mapper vedtakhendelse til baade sakRad og stoenadRad riktig`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L
        val virkningstidspunkt = YearMonth.of(2023, 6)

        every { stoenadRepo.lagreStoenadsrad(any()) } returnsArgument 0
        every { sakRepo.lagreRad(any()) } returnsArgument 0
        coEvery { behandlingKlient.hentDetaljertBehandling(behandlingId) } returns DetaljertBehandling(
            id = behandlingId,
            sak = sakId,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "12312312312",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            status = BehandlingStatus.FATTET_VEDTAK,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            revurderingsaarsak = null,
            revurderingInfo = null,
            prosesstype = Prosesstype.MANUELL,
            enhet = "1111"
        )
        coEvery { behandlingKlient.hentPersongalleri(behandlingId) } returns Persongalleri(
            "12312312312"
        )
        val mockBeregning = Beregning(
            beregningId = UUID.randomUUID(),
            behandlingId = behandlingId,
            type = Beregningstype.BP,
            beregnetDato = Tidspunkt.now(),
            beregningsperioder = listOf()
        )
        coEvery { beregningKlient.hentBeregningForBehandling(behandlingId) } returns mockBeregning

        val tekniskTidForHendelse = LocalDateTime.of(2023, 2, 1, 8, 30)

        val fattetVedtakMaaned = LocalDate.of(2023, 7, 1)
        val fattetTidspunkt = Tidspunkt.ofNorskTidssone(fattetVedtakMaaned, LocalTime.NOON)
        val (registrertSakRad, registrertStoenadRad) = service.registrerStatistikkForVedtak(
            vedtak = vedtak(
                sakId = sakId,
                behandlingId = behandlingId,
                vedtakFattet = VedtakFattet("Saksbehandler", "saksbehandlerEnhet", fattetTidspunkt),
                attestasjon = Attestasjon("Attestant", "attestantEnhet", fattetTidspunkt),
                virk = virkningstidspunkt
            ),
            vedtakHendelse = VedtakHendelse.IVERKSATT,
            tekniskTid = tekniskTidForHendelse
        )

        registrertSakRad shouldNotBe null
        registrertSakRad?.asClue { registrertSak ->
            registrertSak.sakId shouldBe sakId
            registrertSak.sakYtelse shouldBe SakType.BARNEPENSJON.name
            registrertSak.sakUtland shouldBe SakUtland.NASJONAL
            registrertSak.behandlingId shouldBe behandlingId
            registrertSak.tekniskTid shouldBe tekniskTidForHendelse.toTidspunkt()
            registrertSak.ansvarligEnhet shouldBe "attestantEnhet"
            registrertSak.ansvarligBeslutter shouldBe "Attestant"
            registrertSak.saksbehandler shouldBe "Saksbehandler"
            registrertSak.beregning shouldBe mockBeregning
            registrertSak.avkorting shouldBe null
        }

        registrertStoenadRad shouldNotBe null
        registrertStoenadRad?.asClue { registrertStoenad ->
            registrertStoenad.tekniskTid shouldBe tekniskTidForHendelse.toTidspunkt()
            registrertStoenad.beregning shouldBe mockBeregning
            registrertStoenad.avkorting shouldBe null
            registrertStoenad.behandlingId shouldBe behandlingId
            registrertStoenad.sakId shouldBe sakId
            registrertStoenad.attestant shouldBe "Attestant"
            registrertStoenad.saksbehandler shouldBe "Saksbehandler"

            registrertStoenad.sakUtland shouldBe SakUtland.NASJONAL
            registrertStoenad.virkningstidspunkt shouldBe virkningstidspunkt
            registrertStoenad.utbetalingsdato shouldBe fattetVedtakMaaned.plusMonths(1).plusDays(19)
        }
    }

    @Test
    fun `mapper vedtakhendelse for omstillingsstoenad`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L
        val virkningstidspunkt = YearMonth.of(2023, 6)

        every { stoenadRepo.lagreStoenadsrad(any()) } returnsArgument 0
        every { sakRepo.lagreRad(any()) } returnsArgument 0
        coEvery { behandlingKlient.hentDetaljertBehandling(behandlingId) } returns DetaljertBehandling(
            id = behandlingId,
            sak = sakId,
            sakType = SakType.OMSTILLINGSSTOENAD,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "12312312312",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            status = BehandlingStatus.FATTET_VEDTAK,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            revurderingsaarsak = null,
            revurderingInfo = null,
            prosesstype = Prosesstype.MANUELL,
            enhet = "1111"
        )
        coEvery { behandlingKlient.hentPersongalleri(behandlingId) } returns Persongalleri(
            "12312312312"
        )
        val mockBeregning = Beregning(
            beregningId = UUID.randomUUID(),
            behandlingId = behandlingId,
            type = Beregningstype.OMS,
            beregnetDato = Tidspunkt.now(),
            beregningsperioder = listOf()
        )
        coEvery { beregningKlient.hentBeregningForBehandling(behandlingId) } returns mockBeregning
        val mockAvkorting = Avkorting(
            listOf(
                AvkortingGrunnlag(
                    fom = YearMonth.now(),
                    tom = null,
                    aarsinntekt = 100,
                    fratrekkInnAar = 40,
                    relevanteMaanederInnAar = 2,
                    spesifikasjon = ""
                )
            ),
            listOf(
                AvkortetYtelse(
                    fom = YearMonth.now(),
                    tom = null,
                    ytelseFoerAvkorting = 200,
                    avkortingsbeloep = 50,
                    ytelseEtterAvkorting = 150,
                    restanse = 0
                )
            )
        )
        coEvery { beregningKlient.hentAvkortingForBehandling(behandlingId) } returns mockAvkorting

        val fattetTidspunkt = Tidspunkt.ofNorskTidssone(LocalDate.of(2023, 7, 1), LocalTime.NOON)
        val (registrertSakRad, registrertStoenadRad) = service.registrerStatistikkForVedtak(
            vedtak = vedtak(
                sakId = sakId,
                sakType = SakType.OMSTILLINGSSTOENAD,
                behandlingId = behandlingId,
                vedtakFattet = VedtakFattet("Saksbehandler", "saksbehandlerEnhet", fattetTidspunkt),
                attestasjon = Attestasjon("Attestant", "attestantEnhet", fattetTidspunkt),
                virk = virkningstidspunkt
            ),
            vedtakHendelse = VedtakHendelse.IVERKSATT,
            tekniskTid = LocalDateTime.of(2023, 2, 1, 8, 30)
        )

        registrertSakRad shouldNotBe null
        registrertSakRad?.asClue { registrertSak ->
            registrertSak.sakId shouldBe sakId
            registrertSak.sakYtelse shouldBe SakType.OMSTILLINGSSTOENAD.name
            registrertSak.beregning shouldBe mockBeregning
            registrertSak.avkorting shouldBe mockAvkorting
        }
        registrertStoenadRad shouldNotBe null
        registrertStoenadRad?.asClue { registrertStoenad ->
            registrertStoenad.beregning shouldBe mockBeregning
            registrertStoenad.avkorting shouldBe mockAvkorting
        }
    }

    @Test
    fun `mapper behandlinghendelse riktig`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L
        every { stoenadRepo.lagreStoenadsrad(any()) } returnsArgument 0
        every { sakRepo.lagreRad(any()) } returnsArgument 0

        coEvery { behandlingKlient.hentDetaljertBehandling(behandlingId) } returns DetaljertBehandling(
            id = behandlingId,
            sak = sakId,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "12312312312",
            gjenlevende = listOf(),
            avdoed = listOf("32132132132"),
            soesken = listOf(),
            status = BehandlingStatus.OPPRETTET,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            boddEllerArbeidetUtlandet = null,
            revurderingsaarsak = null,
            prosesstype = Prosesstype.MANUELL,
            revurderingInfo = null,
            enhet = "1111"
        )

        val tekniskTidForHendelse = LocalDateTime.of(2023, 2, 1, 8, 30)
        val registrertStatistikk = service.registrerStatistikkForBehandlinghendelse(
            behandlingIntern = behandling(id = behandlingId, sakId = sakId),
            hendelse = BehandlingHendelse.OPPRETTET,
            tekniskTid = tekniskTidForHendelse
        ) ?: throw NullPointerException("Fikk ikke registrert statistikk")

        assertEquals(registrertStatistikk.sakId, sakId)
        assertEquals(registrertStatistikk.sakYtelse, "BARNEPENSJON")
        assertEquals(registrertStatistikk.sakUtland, SakUtland.NASJONAL)
        assertEquals(registrertStatistikk.behandlingId, behandlingId)
        assertEquals(registrertStatistikk.sakYtelsesgruppe, SakYtelsesgruppe.EN_AVDOED_FORELDER)
        assertEquals(registrertStatistikk.tekniskTid, tekniskTidForHendelse.toTidspunkt())
        assertEquals(registrertStatistikk.behandlingMetode, BehandlingMetode.MANUELL)
        assertNull(registrertStatistikk.ansvarligBeslutter)
        assertEquals("1111", registrertStatistikk.ansvarligEnhet)
        assertNull(registrertStatistikk.saksbehandler)
    }

    @Test
    fun `lagreMaanedligStoenadstatistikk lagrer ting riktig`() {
        val stoenadRepository: StoenadRepository = mockk(relaxed = true)
        val service = StatistikkService(
            stoenadRepository = stoenadRepository,
            sakRepository = sakRepo,
            behandlingKlient = behandlingKlient,
            beregningKlient = beregningKlient
        )
        service.lagreMaanedsstatistikk(MaanedStatistikk(YearMonth.of(2022, 8), emptyList()))
        verify {
            stoenadRepository.lagreMaanedJobUtfoert(YearMonth.of(2022, 8), 0, 0)
        }
    }
}

fun vedtak(
    vedtakId: Long = 0,
    virk: YearMonth = YearMonth.of(2022, 8),
    sakId: Long = 0,
    ident: String = "",
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingId: UUID = UUID.randomUUID(),
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    type: VedtakType = VedtakType.INNVILGELSE,
    pensjonTilUtbetaling: List<Utbetalingsperiode>? = null,
    vedtakFattet: VedtakFattet? = null,
    attestasjon: Attestasjon? = null
): VedtakDto = VedtakDto(
    vedtakId = vedtakId,
    status = VedtakStatus.ATTESTERT,
    virkningstidspunkt = virk,
    sak = VedtakSak(ident = ident, sakType = sakType, id = sakId),
    behandling = Behandling(type = behandlingType, id = behandlingId),
    type = type,
    utbetalingsperioder = pensjonTilUtbetaling ?: emptyList(),
    vedtakFattet = vedtakFattet,
    attestasjon = attestasjon
)

fun behandling(
    id: UUID = UUID.randomUUID(),
    sakId: Long = 1L,
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingOpprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    sistEndret: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    soeker: String = "12312312312"
) = BehandlingIntern(
    id = id,
    sak = Sak(soeker, sakType, sakId, "4808"),
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    type = type
)