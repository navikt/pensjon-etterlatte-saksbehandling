package no.nav.etterlatte.statistikk.service

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.SakMedGraderingOgSkjermet
import no.nav.etterlatte.libs.common.sak.VedtakSak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
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
import no.nav.etterlatte.statistikk.domain.stoenadRad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class StatistikkServiceTest {
    private val stoenadRepo = mockk<StoenadRepository>()
    private val sakRepo = mockk<SakRepository>()
    private val behandlingKlient = mockk<BehandlingKlient>()
    private val beregningKlient = mockk<BeregningKlient>()
    private val aktivitetspliktService = mockk<AktivitetspliktService>()
    private val service =
        StatistikkService(
            stoenadRepository = stoenadRepo,
            sakRepository = sakRepo,
            behandlingKlient = behandlingKlient,
            beregningKlient = beregningKlient,
            aktivitetspliktService = aktivitetspliktService,
        )

    @Test
    fun `mapper vedtakhendelse til baade sakRad og stoenadRad riktig`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L
        val virkningstidspunkt = YearMonth.of(2023, 6)
        val enhet = "1111"
        coEvery { behandlingKlient.hentGraderingForSak(sakId) } returns
            SakMedGraderingOgSkjermet(
                id = sakId,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                erSkjermet = false,
                enhetNr = enhet,
            )

        coEvery { behandlingKlient.hentStatistikkBehandling(behandlingId) } returns
            StatistikkBehandling(
                id = behandlingId,
                sak = Sak(id = sakId, sakType = SakType.OMSTILLINGSSTOENAD, enhet = enhet, ident = "ident"),
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
                utlandstilknytning = null,
                enhet = enhet,
                kilde = Vedtaksloesning.GJENNY,
                sistEndret = LocalDateTime.now(),
                pesysId = 123L,
                relatertBehandlingId = null,
            )
        every { stoenadRepo.lagreStoenadsrad(any()) } returnsArgument 0
        every { sakRepo.lagreRad(any()) } returnsArgument 0
        coEvery { behandlingKlient.hentPersongalleri(behandlingId) } returns
            Persongalleri(
                "12312312312",
            )
        val mockBeregning =
            Beregning(
                beregningId = UUID.randomUUID(),
                behandlingId = behandlingId,
                type = Beregningstype.BP,
                beregnetDato = Tidspunkt.now(),
                beregningsperioder = listOf(),
            )
        coEvery { beregningKlient.hentBeregningForBehandling(behandlingId) } returns mockBeregning

        val tekniskTidForHendelse = LocalDateTime.of(2023, 2, 1, 8, 30)

        val fattetVedtakMaaned = LocalDate.of(2023, 7, 1)
        val fattetTidspunkt = Tidspunkt.ofNorskTidssone(fattetVedtakMaaned, LocalTime.NOON)
        val (registrertSakRad, registrertStoenadRad) =
            service.registrerStatistikkForVedtak(
                vedtak =
                    vedtak(
                        sakId = sakId,
                        behandlingId = behandlingId,
                        vedtakFattet = VedtakFattet("Saksbehandler", "saksbehandlerEnhet", fattetTidspunkt),
                        attestasjon = Attestasjon("Attestant", "attestantEnhet", fattetTidspunkt),
                        virk = virkningstidspunkt,
                    ),
                vedtakKafkaHendelseType = VedtakKafkaHendelseHendelseType.IVERKSATT,
                tekniskTid = tekniskTidForHendelse,
            )

        registrertSakRad shouldNotBe null
        registrertSakRad?.asClue { registrertSak ->
            registrertSak.sakId shouldBe sakId
            registrertSak.sakYtelse shouldBe SakType.BARNEPENSJON.name
            registrertSak.sakUtland shouldBe null
            registrertSak.referanseId shouldBe behandlingId
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

            registrertStoenad.sakUtland shouldBe null
            registrertStoenad.virkningstidspunkt shouldBe virkningstidspunkt
            registrertStoenad.utbetalingsdato shouldBe fattetVedtakMaaned.plusMonths(1).plusDays(19)
        }
    }

    @Test
    fun `hopper over mapping av statistikk hvis det er en fortrolig sak`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L
        val virk = YearMonth.of(2024, Month.MAY)
        val fattetTidspunkt = Tidspunkt.now()

        every { sakRepo.lagreRad(any()) } returnsArgument 0
        coEvery { behandlingKlient.hentPersongalleri(behandlingId) } returns
            Persongalleri(
                "12312312312",
            )
        val mockBeregning =
            Beregning(
                beregningId = UUID.randomUUID(),
                behandlingId = behandlingId,
                type = Beregningstype.OMS,
                beregnetDato = Tidspunkt.now(),
                beregningsperioder = listOf(),
            )
        coEvery { beregningKlient.hentBeregningForBehandling(behandlingId) } returns mockBeregning

        coEvery { behandlingKlient.hentGraderingForSak(sakId) } returns
            SakMedGraderingOgSkjermet(
                id = sakId,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.FORTROLIG,
                erSkjermet = false,
                enhetNr = Enhet.defaultEnhet.enhetNr,
            )
        coEvery { behandlingKlient.hentStatistikkBehandling(behandlingId) } returns
            StatistikkBehandling(
                id = behandlingId,
                sak =
                    Sak(
                        id = sakId,
                        sakType = SakType.BARNEPENSJON,
                        enhet = Enhet.defaultEnhet.enhetNr,
                        ident = "ident",
                    ),
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
                enhet = Enhet.defaultEnhet.enhetNr,
                kilde = Vedtaksloesning.GJENNY,
                utlandstilknytning = null,
                sistEndret = LocalDateTime.now(),
                pesysId = null,
                relatertBehandlingId = null,
            )

        val (registrertSakRad, registrertStoenadRad) =
            service.registrerStatistikkForVedtak(
                vedtak =
                    vedtak(
                        sakId = sakId,
                        sakType = SakType.BARNEPENSJON,
                        behandlingId = behandlingId,
                        vedtakFattet = VedtakFattet("Saksbehandler", "saksbehandlerEnhet", fattetTidspunkt),
                        attestasjon = Attestasjon("Attestant", "attestantEnhet", fattetTidspunkt),
                        virk = virk,
                    ),
                vedtakKafkaHendelseType = VedtakKafkaHendelseHendelseType.IVERKSATT,
                tekniskTid = LocalDateTime.of(2023, 2, 1, 8, 30),
            )
        assertNull(registrertStoenadRad)
    }

    @Test
    fun `mapper vedtakhendelse for omstillingsstoenad`() {
        val behandlingId = UUID.randomUUID()
        val sakId = 1L
        val virkningstidspunkt = YearMonth.of(2023, 6)

        every { stoenadRepo.lagreStoenadsrad(any()) } returnsArgument 0
        every { sakRepo.lagreRad(any()) } returnsArgument 0
        coEvery { behandlingKlient.hentPersongalleri(behandlingId) } returns
            Persongalleri(
                "12312312312",
            )
        val mockBeregning =
            Beregning(
                beregningId = UUID.randomUUID(),
                behandlingId = behandlingId,
                type = Beregningstype.OMS,
                beregnetDato = Tidspunkt.now(),
                beregningsperioder = listOf(),
            )
        coEvery { beregningKlient.hentBeregningForBehandling(behandlingId) } returns mockBeregning
        val mockAvkorting =
            Avkorting(
                listOf(
                    AvkortingGrunnlag(
                        fom = YearMonth.now(),
                        tom = null,
                        aarsinntekt = 100,
                        fratrekkInnAar = 40,
                        relevanteMaanederInnAar = 2,
                        spesifikasjon = "",
                    ),
                ),
                listOf(
                    AvkortetYtelse(
                        fom = YearMonth.now(),
                        tom = null,
                        ytelseFoerAvkorting = 200,
                        avkortingsbeloep = 50,
                        ytelseEtterAvkorting = 150,
                        restanse = 0,
                        sanksjonertYtelse = null,
                    ),
                ),
            )
        coEvery { beregningKlient.hentAvkortingForBehandling(behandlingId) } returns mockAvkorting

        coEvery { behandlingKlient.hentGraderingForSak(sakId) } returns
            SakMedGraderingOgSkjermet(
                sakId,
                adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                erSkjermet = false,
                enhetNr = Enhet.defaultEnhet.enhetNr,
            )

        val fattetTidspunkt = Tidspunkt.ofNorskTidssone(LocalDate.of(2023, 7, 1), LocalTime.NOON)
        val enhet = "1111"
        coEvery { behandlingKlient.hentStatistikkBehandling(behandlingId) } returns
            StatistikkBehandling(
                id = behandlingId,
                sak = Sak(id = sakId, sakType = SakType.OMSTILLINGSSTOENAD, enhet = enhet, ident = "ident"),
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
                enhet = enhet,
                kilde = Vedtaksloesning.GJENNY,
                utlandstilknytning = null,
                sistEndret = LocalDateTime.now(),
                pesysId = 123L,
                relatertBehandlingId = null,
            )

        val (registrertSakRad, registrertStoenadRad) =
            service.registrerStatistikkForVedtak(
                vedtak =
                    vedtak(
                        sakId = sakId,
                        sakType = SakType.OMSTILLINGSSTOENAD,
                        behandlingId = behandlingId,
                        vedtakFattet = VedtakFattet("Saksbehandler", "saksbehandlerEnhet", fattetTidspunkt),
                        attestasjon = Attestasjon("Attestant", "attestantEnhet", fattetTidspunkt),
                        virk = virkningstidspunkt,
                    ),
                vedtakKafkaHendelseType = VedtakKafkaHendelseHendelseType.IVERKSATT,
                tekniskTid = LocalDateTime.of(2023, 2, 1, 8, 30),
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

        val tekniskTidForHendelse = LocalDateTime.of(2023, 2, 1, 8, 30)
        val registrertStatistikk =
            service.registrerStatistikkForBehandlinghendelse(
                statistikkBehandling = behandling(id = behandlingId, sakId = sakId, avdoed = listOf("etfnr")),
                hendelse = BehandlingHendelseType.OPPRETTET,
                tekniskTid = tekniskTidForHendelse,
            ) ?: throw NullPointerException("Fikk ikke registrert statistikk")

        assertEquals(registrertStatistikk.sakId, sakId)
        assertEquals(registrertStatistikk.sakYtelse, "BARNEPENSJON")
        assertEquals(registrertStatistikk.sakUtland, SakUtland.NASJONAL)
        assertEquals(registrertStatistikk.referanseId, behandlingId)
        assertEquals(registrertStatistikk.sakYtelsesgruppe, SakYtelsesgruppe.EN_AVDOED_FORELDER)
        assertEquals(registrertStatistikk.tekniskTid, tekniskTidForHendelse.toTidspunkt())
        assertEquals(registrertStatistikk.behandlingMetode, BehandlingMetode.MANUELL)
        assertNull(registrertStatistikk.ansvarligBeslutter)
        assertEquals("4808", registrertStatistikk.ansvarligEnhet)
        assertNull(registrertStatistikk.saksbehandler)
    }

    @Test
    fun `lagreMaanedligStoenadstatistikk lagrer ting riktig`() {
        val stoenadRepository: StoenadRepository = mockk(relaxed = true)
        val service =
            StatistikkService(
                stoenadRepository = stoenadRepository,
                sakRepository = sakRepo,
                behandlingKlient = behandlingKlient,
                beregningKlient = beregningKlient,
                aktivitetspliktService = aktivitetspliktService,
            )
        service.lagreMaanedsstatistikk(MaanedStatistikk(YearMonth.of(2022, 8), emptyList(), emptyMap()))
        verify {
            stoenadRepository.lagreMaanedJobUtfoert(YearMonth.of(2022, 8), 0, 0)
        }
    }

    @Test
    fun `produserStoenadStatistikkForMaaned henter aktivitetspliktdata for omstillingsstønad-vedtak`() {
        val stoenadRepository = mockk<StoenadRepository>(relaxed = true)
        val mockAktivitetspliktService = mockk<AktivitetspliktService>()

        val omsSakId = 10000L..11231L
        val bpSakId = 5000L..7000L

        val omsStoenadRad =
            omsSakId.map {
                stoenadRad(sakId = it, sakYtelse = SakType.OMSTILLINGSSTOENAD.name)
            }
        val bpStoenadRad =
            bpSakId.map {
                stoenadRad(sakId = it, sakYtelse = SakType.BARNEPENSJON.name)
            }
        val service =
            StatistikkService(
                stoenadRepository = stoenadRepository,
                sakRepository = sakRepo,
                behandlingKlient = behandlingKlient,
                beregningKlient = beregningKlient,
                aktivitetspliktService = mockAktivitetspliktService,
            )
        val brukteOmsIder = slot<List<Long>>()

        val statistikkMaaned = YearMonth.of(2024, Month.JULY)
        every { stoenadRepository.hentStoenadRaderInnenforMaaned(statistikkMaaned) } returns omsStoenadRad + bpStoenadRad
        every {
            mockAktivitetspliktService.mapAktivitetForSaker(
                capture(brukteOmsIder),
                statistikkMaaned,
            )
        } returns emptyMap()

        service.produserStoenadStatistikkForMaaned(statistikkMaaned)

        brukteOmsIder.captured.shouldContainAll(omsSakId.map { it })
        brukteOmsIder.captured.shouldNotContainAnyOf(bpSakId.map { it })
    }
}

fun vedtak(
    vedtakId: Long = 0,
    virk: YearMonth = YearMonth.of(2022, 8),
    sakId: SakId = 0,
    ident: String = "",
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingId: UUID = UUID.randomUUID(),
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    type: VedtakType = VedtakType.INNVILGELSE,
    pensjonTilUtbetaling: List<Utbetalingsperiode>? = null,
    vedtakFattet: VedtakFattet? = null,
    attestasjon: Attestasjon? = null,
    opphoerFom: YearMonth? = null,
) = VedtakDto(
    id = vedtakId,
    behandlingId = behandlingId,
    status = VedtakStatus.ATTESTERT,
    sak = VedtakSak(ident = ident, sakType = sakType, id = sakId),
    type = type,
    vedtakFattet = vedtakFattet,
    attestasjon = attestasjon,
    innhold =
        VedtakInnholdDto.VedtakBehandlingDto(
            virkningstidspunkt = virk,
            behandling = Behandling(type = behandlingType, id = behandlingId),
            utbetalingsperioder = pensjonTilUtbetaling ?: emptyList(),
            opphoerFraOgMed = opphoerFom,
        ),
)

fun behandling(
    id: UUID = UUID.randomUUID(),
    sakId: SakId = 1L,
    sakType: SakType = SakType.BARNEPENSJON,
    behandlingOpprettet: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    sistEndret: LocalDateTime = Tidspunkt.now().toLocalDatetimeUTC(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    soeker: String = "12312312312",
    avdoed: List<String>? = null,
) = StatistikkBehandling(
    id = id,
    sak = Sak(soeker, sakType, sakId, Enhet.defaultEnhet.enhetNr),
    behandlingOpprettet = behandlingOpprettet,
    sistEndret = sistEndret,
    status = status,
    behandlingType = type,
    gjenlevende = null,
    avdoed = avdoed,
    boddEllerArbeidetUtlandet = null,
    soeknadMottattDato = LocalDateTime.now(),
    innsender = "Sss",
    soeker = "soeker",
    soesken = null,
    virkningstidspunkt =
        Virkningstidspunkt.create(
            YearMonth.now(),
            "begrunnelse",
            saksbehandler = Grunnlagsopplysning.Saksbehandler.create("ident"),
        ),
    enhet = Enhet.defaultEnhet.enhetNr,
    revurderingsaarsak = null,
    revurderingInfo = null,
    prosesstype = Prosesstype.MANUELL,
    utlandstilknytning = null,
    kilde = Vedtaksloesning.GJENNY,
    pesysId = 123L,
    relatertBehandlingId = null,
)
