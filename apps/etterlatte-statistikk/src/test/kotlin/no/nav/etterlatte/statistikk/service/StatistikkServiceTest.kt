package no.nav.etterlatte.statistikk.service

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.Beregningstype
import no.nav.etterlatte.statistikk.domain.MaanedStatistikk
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.river.BehandlingHendelse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

class StatistikkServiceTest {

    @Test
    fun `mapper vedtakhendelse til både sakRad og stoenadRad riktig`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L

        val stoenadRepo = mockk<StoenadRepository>()
        every { stoenadRepo.lagreStoenadsrad(any()) } returnsArgument 0

        val sakRepo = mockk<SakRepository>()
        every { sakRepo.lagreRad(any()) } returnsArgument 0

        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentDetaljertBehandling(behandlingId) } returns DetaljertBehandling(
            id = behandlingId,
            sak = sakId,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "12312312312",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            gyldighetsproeving = null,
            status = BehandlingStatus.FATTET_VEDTAK,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            kommerBarnetTilgode = null,
            revurderingsaarsak = null
        )
        coEvery { behandlingKlient.hentPersongalleri(behandlingId) } returns Persongalleri(
            "12312312312"
        )
        val mockBeregning = Beregning(
            beregningId = UUID.randomUUID(),
            behandlingId = behandlingId,
            type = Beregningstype.BP,
            beregnetDato = Tidspunkt(instant = Instant.now()),
            beregningsperioder = listOf()
        )
        val beregningKlient = mockk<BeregningKlient>()
        coEvery { beregningKlient.hentBeregningForBehandling(behandlingId) } returns mockBeregning

        val service = StatistikkService(
            stoenadRepository = stoenadRepo,
            sakRepository = sakRepo,
            behandlingKlient = behandlingKlient,
            beregningKlient = beregningKlient
        )
        val tekniskTidForHendelse = LocalDateTime.of(2023, 2, 1, 8, 30)

        val (registrertSakRad, registrertStoenadRad) = service.registrerStatistikkForVedtak(
            vedtakDto = vedtak(
                sakId = sakId,
                behandlingId = behandlingId,
                vedtakFattet = VedtakFattet("Saksbehandler", "saksbehandlerEnhet", ZonedDateTime.now()),
                attestasjon = Attestasjon("Attestant", "attestantEnhet", ZonedDateTime.now())
            ),
            vedtakHendelse = VedtakHendelse.IVERKSATT,
            tekniskTid = tekniskTidForHendelse
        )

        // denne gjør at kotlin kan inferre at de ikke er null, så det ikke blir ? i alle assertions under
        if (registrertStoenadRad == null || registrertSakRad == null) {
            throw NullPointerException("Stønadrad=$registrertStoenadRad eller sakrad=$registrertSakRad var null")
        }

        Assertions.assertEquals(registrertSakRad.sakId, sakId)
        Assertions.assertEquals(registrertSakRad.sakYtelse, "BARNEPENSJON")
        Assertions.assertEquals(registrertSakRad.sakUtland, SakUtland.NASJONAL)
        Assertions.assertEquals(registrertSakRad.behandlingId, behandlingId)
        Assertions.assertEquals(registrertSakRad.tekniskTid, tekniskTidForHendelse.toTidspunkt())
        Assertions.assertEquals(registrertSakRad.ansvarligEnhet, "attestantEnhet")
        Assertions.assertEquals(registrertSakRad.ansvarligBeslutter, "Attestant")
        Assertions.assertEquals(registrertSakRad.saksbehandler, "Saksbehandler")
        Assertions.assertEquals(registrertSakRad.beregning, mockBeregning)

        Assertions.assertEquals(registrertStoenadRad.tekniskTid, tekniskTidForHendelse.toTidspunkt())
        Assertions.assertEquals(registrertStoenadRad.beregning, mockBeregning)
        Assertions.assertEquals(registrertStoenadRad.behandlingId, behandlingId)
        Assertions.assertEquals(registrertStoenadRad.sakId, sakId)
        Assertions.assertEquals(registrertStoenadRad.attestant, "Attestant")
        Assertions.assertEquals(registrertStoenadRad.saksbehandler, "Saksbehandler")
    }

    @Test
    fun `mapper behandlinghendelse riktig`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L
        val stoenadRepo = mockk<StoenadRepository>()
        every { stoenadRepo.lagreStoenadsrad(any()) } returnsArgument 0

        val sakRepo = mockk<SakRepository>()
        every { sakRepo.lagreRad(any()) } returnsArgument 0

        val behandlingKlient = mockk<BehandlingKlient>()
        coEvery { behandlingKlient.hentDetaljertBehandling(behandlingId) } returns DetaljertBehandling(
            id = behandlingId,
            sak = sakId,
            sakType = SakType.BARNEPENSJON,
            behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
            sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
            soeknadMottattDato = null,
            innsender = null,
            soeker = "12312312312",
            gjenlevende = listOf(),
            avdoed = listOf(),
            soesken = listOf(),
            gyldighetsproeving = null,
            status = BehandlingStatus.OPPRETTET,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            virkningstidspunkt = null,
            kommerBarnetTilgode = null,
            revurderingsaarsak = null
        )
        coEvery { behandlingKlient.hentSak(sakId) } returns Sak(
            ident = "12312312312",
            sakType = SakType.BARNEPENSJON,
            id = sakId
        )

        val beregningKlient = mockk<BeregningKlient>()

        val service = StatistikkService(
            stoenadRepository = stoenadRepo,
            sakRepository = sakRepo,
            behandlingKlient = behandlingKlient,
            beregningKlient = beregningKlient
        )

        val tekniskTidForHendelse = LocalDateTime.of(2023, 2, 1, 8, 30)
        val registrertStatistikk = service.registrerStatistikkForBehandlinghendelse(
            behandlingIntern = behandling(id = behandlingId, sakId = sakId),
            hendelse = BehandlingHendelse.OPPRETTET,
            tekniskTid = tekniskTidForHendelse
        ) ?: throw NullPointerException("Fikk ikke registrert statistikk")

        Assertions.assertEquals(registrertStatistikk.sakId, sakId)
        Assertions.assertEquals(registrertStatistikk.sakYtelse, "BARNEPENSJON")
        Assertions.assertEquals(registrertStatistikk.sakUtland, SakUtland.NASJONAL)
        Assertions.assertEquals(registrertStatistikk.behandlingId, behandlingId)
        Assertions.assertEquals(registrertStatistikk.tekniskTid, tekniskTidForHendelse.toTidspunkt())
        Assertions.assertNull(registrertStatistikk.behandlingMetode)
        Assertions.assertNull(registrertStatistikk.ansvarligBeslutter)
        Assertions.assertNull(registrertStatistikk.ansvarligEnhet)
        Assertions.assertNull(registrertStatistikk.saksbehandler)
    }

    @Test
    fun `lagreMaanedligStoenadstatistikk lagrer ting riktig`() {
        val stoenadRepository: StoenadRepository = mockk(relaxed = true)

        val sakRepository: SakRepository = mockk()
        val behandlingKlient: BehandlingKlient = mockk()
        val beregningKlient: BeregningKlient = mockk()

        val service = StatistikkService(
            stoenadRepository = stoenadRepository,
            sakRepository = sakRepository,
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
    virkningstidspunkt = virk,
    sak = Sak(ident = ident, sakType = sakType, id = sakId),
    behandling = Behandling(type = behandlingType, id = behandlingId),
    type = type,
    utbetalingsperioder = pensjonTilUtbetaling ?: emptyList(),
    vedtakFattet = vedtakFattet,
    attestasjon = attestasjon
)

fun behandling(
    id: UUID = UUID.randomUUID(),
    sakId: Long = 1L,
    behandlingOpprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    sistEndret: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    soeker: String = "12312312312",
    innsender: String? = null,
    soesken: List<String> = emptyList(),
    avdoed: List<String> = emptyList(),
    gjenlevende: List<String> = emptyList()
): no.nav.etterlatte.statistikk.river.BehandlingIntern = no.nav.etterlatte.statistikk.river.BehandlingIntern(
    id = id,
    sakId = sakId,
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    type = type,
    persongalleri = Persongalleri(
        soeker = soeker,
        innsender = innsender,
        soesken = soesken,
        avdoed = avdoed,
        gjenlevende = gjenlevende
    )
)