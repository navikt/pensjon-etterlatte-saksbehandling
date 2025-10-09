package no.nav.etterlatte.statistikk.service

import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.PaaVentAarsak
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatistikkDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.StatistikkTilbakekrevingDto
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingHendelseType
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingResultat
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingStatus
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.database.EtteroppgjoerRad
import no.nav.etterlatte.statistikk.database.KjoertStatus
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.domain.Avkorting
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.BehandlingResultat
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.MaanedStatistikk
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import no.nav.etterlatte.statistikk.domain.SoeknadFormat
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.domain.tilStoenadUtbetalingsperiode
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

class StatistikkService(
    private val stoenadRepository: StoenadRepository,
    private val sakRepository: SakRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregningKlient: BeregningKlient,
    private val aktivitetspliktService: AktivitetspliktService,
    private val etteroppgjoerService: EtteroppgjoerStatistikkService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun registrerStatistikkForVedtak(
        vedtak: VedtakDto,
        vedtakKafkaHendelseType: VedtakKafkaHendelseHendelseType,
        tekniskTid: LocalDateTime,
    ): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtak, vedtakKafkaHendelseType, tekniskTid)
        if (vedtakKafkaHendelseType == VedtakKafkaHendelseHendelseType.IVERKSATT) {
            val enhet = vedtak.attestasjon?.attesterendeEnhet
            if (enhet in listOf(Enheter.STRENGT_FORTROLIG.enhetNr, Enheter.STRENGT_FORTROLIG_UTLAND.enhetNr)) {
                return sakRad to null
            }
            val gradering = runBlocking { behandlingKlient.hentGraderingForSak(vedtak.sak.id) }
            when (gradering.adressebeskyttelseGradering) {
                AdressebeskyttelseGradering.FORTROLIG,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
                -> return sakRad to null

                null, AdressebeskyttelseGradering.UGRADERT -> Unit
            }
            val stoenadRad =
                when (vedtak.type) {
                    VedtakType.INNVILGELSE,
                    VedtakType.ENDRING,
                    VedtakType.OPPHOER,
                    ->
                        stoenadRepository
                            .lagreStoenadsrad(
                                vedtakTilStoenadsrad(
                                    vedtak,
                                    tekniskTid,
                                    sakRad!!.vedtaksloesning,
                                    sakRad.pesysId,
                                ),
                            ).also {
                                hentSisteAktivitetspliktDto(vedtak)
                            }

                    VedtakType.TILBAKEKREVING,
                    VedtakType.AVVIST_KLAGE,
                    VedtakType.AVSLAG,
                    -> null
                }
            return sakRad to stoenadRad
        }
        return sakRad to null
    }

    private fun hentSisteAktivitetspliktDto(vedtak: VedtakDto) {
        try {
            if (vedtak.sak.sakType == SakType.OMSTILLINGSSTOENAD) {
                val aktivitetspliktDto =
                    runBlocking { behandlingKlient.hentAktivitetspliktDto(vedtak.sak.id, vedtak.behandlingId) }
                aktivitetspliktService.oppdaterVurderingAktivitetsplikt(aktivitetspliktDto)
            }
        } catch (e: Exception) {
            // TODO bruke exceptionen?
            logger.error(
                "Kunne ikke hente og oppdatere aktivitetspliktstatusen for OMS-sak med id=${vedtak.sak.id}" +
                    "Dette betyr at vi ikke kjenner til den oppdaterte aktivitetspliktstatusen for saken," +
                    "og trenger å følges opp for å få de riktig med i statistikken.",
            )
        }
    }

    fun produserStoenadStatistikkForMaaned(maaned: YearMonth): MaanedStatistikk {
        val vedtak = stoenadRepository.hentStoenadRaderInnenforMaaned(maaned)
        val omsSaker = vedtak.filter { it.sakYtelse == SakType.OMSTILLINGSSTOENAD.name }.map { it.sakId }
        val aktiviteterForOmsSaker = aktivitetspliktService.mapAktivitetForSaker(omsSaker, maaned)
        return MaanedStatistikk(maaned, vedtak, aktiviteterForOmsSaker)
    }

    fun registrerStatistikkForBehandlinghendelse(
        statistikkBehandling: StatistikkBehandling,
        hendelse: BehandlingHendelseType,
        tekniskTid: LocalDateTime,
    ): SakRad? {
        if (hendelse == BehandlingHendelseType.OPPRETTET) {
            // Dette kan potensielt være en behandling som blir rullet tilbake. Vi vil gjøre ekstra verifisering på
            // at behandlingen finnes
            try {
                runBlocking { retry(3) { behandlingKlient.hentStatistikkBehandling(statistikkBehandling.id) } }
            } catch (e: Exception) {
                // TODO bruke exceptionen?
                logger.error(
                    "Kunne ikke hente behandling med id ${statistikkBehandling.id} fra behandling. " +
                        "Hvis opprettelse av behandlingen ble rullet tilbake og behandlingen med id=" +
                        "${statistikkBehandling.id} ikke finnes i behandlingsbasen lengre, må det også legges inn " +
                        "en avbrytelsesmelding til statistikk. ",
                )
            }
        }
        return sakRepository.lagreRad(behandlingTilSakRad(statistikkBehandling, hendelse, tekniskTid))
    }

    fun registrerEndretEnhetForReferanse(
        referanse: UUID,
        nyEnhet: Enhetsnummer,
        tekniskTid: LocalDateTime,
    ): SakRad? {
        val sisteRadForReferanse = sakRepository.hentSisteRad(referanse)
        if (sisteRadForReferanse == null) {
            logger.warn("Fikk melding om behandling statistikk ikke kjenner til, med referanse $referanse")
            return null
        }

        return if (sisteRadForReferanse.ansvarligEnhet == nyEnhet) {
            logger.info(
                "Behandlingen med referanse $referanse har allerede enhet $nyEnhet på siste registrering. " +
                    "Oppdaterer ikke statistikken.",
            )
            null
        } else {
            val tekniskTidForOppdatertEnhet =
                if (sisteRadForReferanse.tekniskTid >= tekniskTid.toTidspunkt()) {
                    sisteRadForReferanse.tekniskTid.plus(1, ChronoUnit.SECONDS)
                } else {
                    tekniskTid.toTidspunkt()
                }
            val nyRad =
                sisteRadForReferanse.copy(
                    ansvarligEnhet = nyEnhet,
                    status = BehandlingHendelseType.ENDRET_ENHET.name,
                    tekniskTid = tekniskTidForOppdatertEnhet,
                )
            return sakRepository.lagreRad(nyRad)
        }
    }

    fun registrerStatistikkBehandlingPaaVentHendelse(
        behandlingId: UUID,
        hendelse: BehandlingHendelseType,
        tekniskTid: LocalDateTime,
        aarsak: PaaVentAarsak,
    ): SakRad? {
        val sisteRad =
            sakRepository.hentSisteRad(behandlingId)?.copy(
                status = hendelse.name,
                tekniskTid = tekniskTid.toTidspunkt(),
                paaVentAarsak = aarsak,
            )
        if (sisteRad == null) {
            logger.warn(
                "Registrerte ikke behandling på vent fordi det ble ikke funnet en tidligere rad knyttet til behandlig=$behandlingId",
            )
            return null
        }
        return sakRepository.lagreRad(sisteRad)
    }

    fun registrerStatistikkForKlagehendelse(
        statistikkKlage: StatistikkKlage,
        tekniskTid: LocalDateTime,
        hendelse: KlageHendelseType,
    ): SakRad? = sakRepository.lagreRad(klageTilSakRad(statistikkKlage, tekniskTid, hendelse))

    fun registrerStatistikkFortilbakkrevinghendelse(
        statistikkTilbakekreving: StatistikkTilbakekrevingDto,
        tekniskTid: LocalDateTime,
        hendelse: TilbakekrevingHendelseType,
    ): SakRad? = sakRepository.lagreRad(tilbakekrevingTilSakRad(statistikkTilbakekreving, tekniskTid, hendelse))

    fun statistikkProdusertForMaaned(maaned: YearMonth): KjoertStatus = stoenadRepository.kjoertStatusForMaanedsstatistikk(maaned)

    fun lagreMaanedsstatistikk(maanedsstatistikkk: MaanedStatistikk) {
        var raderMedFeil = 0L
        var raderRegistrert = 0L
        maanedsstatistikkk.rader.forEach {
            try {
                stoenadRepository.lagreMaanedStatistikkRad(it)
                raderRegistrert += 1
            } catch (e: Exception) {
                logger.warn("Maanedsstatistikk for sak med id=${it.sakId} kunne ikke lagres", e)
                raderMedFeil += 1
            }
        }
        stoenadRepository.lagreMaanedJobUtfoert(
            maanedsstatistikkk.maaned,
            raderMedFeil,
            raderRegistrert,
        )
    }

    fun registrerEtteroppgjoerHendelse(
        hendelse: EtteroppgjoerHendelseType,
        statistikkDto: EtteroppgjoerForbehandlingStatistikkDto,
        tekniskTid: Tidspunkt,
        resultat: BeregnetEtteroppgjoerResultatDto?,
    ) {
        val etteroppgjoerStatistikk =
            etteroppgjoerService.registrerEtteroppgjoerHendelse(
                hendelse = hendelse,
                statistikkDto = statistikkDto,
                tekniskTid = tekniskTid,
                resultat = resultat,
            )

        val sakRad =
            etteroppgjoerRadTilSakRad(
                etteroppgjoerRad = etteroppgjoerStatistikk,
                statistikkDto = statistikkDto,
            )
        sakRepository.lagreRad(sakRad)
        logger.info("Registrerte saksstatistikk og etteroppgjoersstatistikk for forbehandling")
    }

    private fun etteroppgjoerRadTilSakRad(
        etteroppgjoerRad: EtteroppgjoerRad,
        statistikkDto: EtteroppgjoerForbehandlingStatistikkDto,
    ): SakRad {
        val sisteResultat =
            when (etteroppgjoerRad.hendelse) {
                EtteroppgjoerHendelseType.FERDIGSTILT ->
                    etteroppgjoerService.hentNyesteBeregnedeResultat(etteroppgjoerRad.forbehandlingId)
                else -> null
            }
        val foersteMaanedIEtteroppgjoer = etteroppgjoerRad.maanederYtelse.min()
        val sisteMaanedIEtteroppgjoeor = etteroppgjoerRad.maanederYtelse.max()

        return SakRad(
            id = -1,
            referanseId = etteroppgjoerRad.forbehandlingId,
            sakId = etteroppgjoerRad.sakId,
            mottattTidspunkt = etteroppgjoerRad.opprettet, // TODO: denne kunne tenkelig være dato for skatteoppgjør
            registrertTidspunkt = etteroppgjoerRad.opprettet,
            ferdigbehandletTidspunkt =
                etteroppgjoerRad.tekniskTid.takeIf {
                    etteroppgjoerRad.hendelse ==
                        EtteroppgjoerHendelseType.FERDIGSTILT
                },
            vedtakTidspunkt = null,
            type = "ETTEROPPGJOER_FORBEHANDLING",
            status = etteroppgjoerRad.hendelse.name,
            resultat = etteroppgjoerRad.resultatType?.name ?: sisteResultat?.name,
            resultatBegrunnelse = null,
            behandlingMetode = BehandlingMetode.MANUELL, // TODO: hvis vi setter på automatisering her må vi skille her
            soeknadFormat = SoeknadFormat.DIGITAL,
            opprettetAv = "GJENNY",
            ansvarligBeslutter = null,
            aktorId = statistikkDto.forbehandling.sak.ident,
            datoFoersteUtbetaling = YearMonth.of(etteroppgjoerRad.aar, etteroppgjoerRad.maanederYtelse.min()).atDay(1),
            tekniskTid = etteroppgjoerRad.tekniskTid,
            sakYtelse = SakType.OMSTILLINGSSTOENAD.name,
            vedtakLoependeFom = YearMonth.of(etteroppgjoerRad.aar, foersteMaanedIEtteroppgjoer).atDay(1),
            vedtakLoependeTom = YearMonth.of(etteroppgjoerRad.aar, sisteMaanedIEtteroppgjoeor).atEndOfMonth(),
            saksbehandler = statistikkDto.saksbehandler,
            ansvarligEnhet = statistikkDto.forbehandling.sak.enhet,
            sakUtland = statistikkDto.utlandstilknytningType?.let { SakUtland.fraUtlandstilknytningType(it) },
            sakUtlandEnhet = SakUtland.fraEnhetsnummer(statistikkDto.forbehandling.sak.enhet),
            beregning = null,
            avkorting = null,
            sakYtelsesgruppe = null,
            avdoedeForeldre = null,
            revurderingAarsak = null,
            vedtaksloesning = Vedtaksloesning.GJENNY,
            pesysId = null,
            relatertTil = statistikkDto.forbehandling.sisteIverksatteBehandlingId.toString(),
        )
    }

    private fun registrerSakStatistikkForVedtak(
        vedtak: VedtakDto,
        hendelse: VedtakKafkaHendelseHendelseType,
        tekniskTid: LocalDateTime,
    ): SakRad? =
        vedtakshendelseTilSakRad(vedtak, hendelse, tekniskTid)?.let { sakRad ->
            sakRepository.lagreRad(sakRad)
        }

    private fun hentBeregningForBehandling(behandlingId: UUID): Beregning? =
        runBlocking {
            beregningKlient.hentBeregningForBehandling(behandlingId)
        }

    private fun hentAvkortingForBehandling(vedtak: VedtakDto): Avkorting? {
        if (vedtak.sak.sakType == SakType.OMSTILLINGSSTOENAD) {
            return runBlocking {
                beregningKlient.hentAvkortingForBehandling(vedtak.behandlingId)
            }
        }
        return null
    }

    private fun vedtakshendelseTilSakRad(
        vedtak: VedtakDto,
        hendelse: VedtakKafkaHendelseHendelseType,
        tekniskTid: LocalDateTime,
    ): SakRad? {
        val vedtakInnhold = (vedtak.innhold as? VedtakInnholdDto.VedtakBehandlingDto) ?: return null
        val statistikkBehandling = hentStatistikkBehandling(vedtak.behandlingId)
        val mottattTid = statistikkBehandling.soeknadMottattDato ?: statistikkBehandling.behandlingOpprettet
        val (beregning, avkorting) =
            when (hendelse) {
                VedtakKafkaHendelseHendelseType.FATTET,
                VedtakKafkaHendelseHendelseType.ATTESTERT,
                VedtakKafkaHendelseHendelseType.IVERKSATT,
                ->
                    Pair(
                        hentBeregningForBehandling(statistikkBehandling.id),
                        hentAvkortingForBehandling(vedtak),
                    )

                else -> Pair(null, null)
            }

        val foersteUtbetaling =
            if (vedtak.type == VedtakType.INNVILGELSE) {
                vedtak.vedtakFattet?.let {
                    vedtakInnhold.utbetalingsperioder.minByOrNull { it.periode.fom }
                }
            } else {
                null
            }

        return SakRad(
            id = -1,
            referanseId = vedtakInnhold.behandling.id,
            sakId = vedtak.sak.id,
            mottattTidspunkt = mottattTid.toTidspunkt(),
            registrertTidspunkt = statistikkBehandling.behandlingOpprettet.toTidspunkt(),
            ferdigbehandletTidspunkt = vedtak.attestasjon?.tidspunkt,
            vedtakTidspunkt = vedtak.attestasjon?.tidspunkt,
            type = vedtakInnhold.behandling.type.name,
            status = hendelse.name,
            resultat = behandlingResultatFraVedtak(vedtak, hendelse, statistikkBehandling)?.name,
            resultatBegrunnelse = null,
            behandlingMetode =
                hentBehandlingMetode(
                    vedtak.attestasjon,
                    statistikkBehandling.prosesstype,
                    statistikkBehandling.revurderingsaarsak,
                ),
            soeknadFormat = SoeknadFormat.DIGITAL,
            opprettetAv = "GJENNY",
            ansvarligBeslutter = vedtak.attestasjon?.attestant,
            aktorId = vedtak.sak.ident,
            datoFoersteUtbetaling = foersteUtbetaling?.periode?.fom?.atDay(1),
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtak.sak.sakType.name,
            vedtakLoependeFom = vedtakInnhold.virkningstidspunkt.atDay(1),
            vedtakLoependeTom = vedtakInnhold.virkningstidspunkt.atEndOfMonth(),
            saksbehandler = vedtak.vedtakFattet?.ansvarligSaksbehandler,
            ansvarligEnhet = vedtak.attestasjon?.attesterendeEnhet ?: statistikkBehandling.enhet,
            sakUtland = SakUtland.fraUtlandstilknytning(statistikkBehandling.utlandstilknytning),
            sakUtlandEnhet = SakUtland.fraEnhetsnummer(statistikkBehandling.sak.enhet),
            beregning = beregning,
            avkorting = avkorting,
            sakYtelsesgruppe =
                hentSakYtelsesgruppe(
                    statistikkBehandling.sak.sakType,
                    statistikkBehandling.avdoed ?: emptyList(),
                ),
            avdoedeForeldre = statistikkBehandling.avdoed,
            revurderingAarsak = statistikkBehandling.revurderingsaarsak?.name,
            vedtaksloesning = statistikkBehandling.kilde,
            pesysId = statistikkBehandling.pesysId,
            relatertTil = statistikkBehandling.relatertBehandlingId,
        )
    }

    private fun hentStatistikkBehandling(behandlingId: UUID) =
        runBlocking {
            behandlingKlient.hentStatistikkBehandling(behandlingId)
        }

    private fun hentPersongalleri(behandlingId: UUID): Persongalleri =
        runBlocking {
            behandlingKlient.hentPersongalleri(behandlingId)
        }

    private fun vedtakTilStoenadsrad(
        vedtak: VedtakDto,
        tekniskTid: LocalDateTime,
        vedtaksloesning: Vedtaksloesning,
        pesysid: Long?,
    ): StoenadRad {
        val persongalleri = hentPersongalleri(behandlingId = vedtak.behandlingId)

        val beregning = hentBeregningForBehandling(vedtak.behandlingId)
        val avkorting = hentAvkortingForBehandling(vedtak)
        val statistikkBehandling = hentStatistikkBehandling(vedtak.behandlingId)
        val utbetalingsdato =
            vedtak.vedtakFattet?.tidspunkt?.let {
                val vedtattDato = it.toLocalDate()
                YearMonth.of(vedtattDato.year, vedtattDato.monthValue).plusMonths(1).atDay(20)
            }
        val vedtakInnhold = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto)
        val vedtaksperioder = vedtakInnhold.utbetalingsperioder.map { tilStoenadUtbetalingsperiode(it) }

        return StoenadRad(
            id = -1,
            fnrSoeker = vedtak.sak.ident,
            fnrForeldre = persongalleri.avdoed,
            fnrSoesken = persongalleri.soesken,
            anvendtTrygdetid = "40",
            // TODO  Bør utbedres?
            nettoYtelse =
                vedtakInnhold.utbetalingsperioder
                    .firstOrNull()
                    ?.beloep
                    ?.toString(),
            beregningType = "FOLKETRYGD",
            anvendtSats = "",
            behandlingId = vedtakInnhold.behandling.id,
            sakId = vedtak.sak.id,
            sakNummer = vedtak.sak.id.sakId,
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtak.sak.sakType.toString(),
            versjon = "",
            saksbehandler = vedtak.vedtakFattet!!.ansvarligSaksbehandler,
            attestant = vedtak.attestasjon?.attestant,
            vedtakLoependeFom = vedtakInnhold.virkningstidspunkt.atDay(1),
            vedtakLoependeTom = null,
            beregning = beregning,
            avkorting = avkorting,
            vedtakType = vedtak.type,
            sakUtland = SakUtland.fraUtlandstilknytning(statistikkBehandling.utlandstilknytning),
            virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
            utbetalingsdato = utbetalingsdato,
            vedtaksloesning = vedtaksloesning,
            pesysId = pesysid,
            sakYtelsesgruppe = hentSakYtelsesgruppe(sakType = vedtak.sak.sakType, avdoede = persongalleri.avdoed),
            opphoerFom = vedtakInnhold.opphoerFraOgMed,
            vedtaksperioder = vedtaksperioder,
        )
    }

    private fun tilbakekrevingTilSakRad(
        statistikkTilbakekreving: StatistikkTilbakekrevingDto,
        tekniskTid: LocalDateTime,
        hendelse: TilbakekrevingHendelseType,
    ) = SakRad(
        id = -1,
        referanseId = statistikkTilbakekreving.id,
        sakId = statistikkTilbakekreving.tilbakekreving.sak.id,
        mottattTidspunkt = statistikkTilbakekreving.tilbakekreving.opprettet,
        registrertTidspunkt = statistikkTilbakekreving.tilbakekreving.opprettet,
        type = "TILBAKEKREVING",
        status = hendelse.name,
        ansvarligEnhet = statistikkTilbakekreving.tilbakekreving.sak.enhet,
        resultat =
            when (statistikkTilbakekreving.tilbakekreving.status) {
                TilbakekrevingStatus.ATTESTERT -> mapTilbakekrevingResultat(statistikkTilbakekreving.tilbakekreving.tilbakekreving)?.name
                TilbakekrevingStatus.AVBRUTT -> "AVBRUTT"
                else -> null
            },
        tekniskTid = tekniskTid.toTidspunkt(),
        sakYtelse = statistikkTilbakekreving.tilbakekreving.sak.sakType.name,
        behandlingMetode = BehandlingMetode.MANUELL,
        avdoedeForeldre = null,
        sakYtelsesgruppe = null,
        revurderingAarsak = null,
        vedtakLoependeFom = null,
        vedtakTidspunkt = null,
        beregning = null,
        avkorting = null,
        datoFoersteUtbetaling = null,
        opprettetAv = null,
        pesysId = null,
        resultatBegrunnelse = null,
        sakUtland = statistikkTilbakekreving.utlandstilknytningType?.let { SakUtland.fraUtlandstilknytningType(it) },
        sakUtlandEnhet = SakUtland.fraEnhetsnummer(statistikkTilbakekreving.tilbakekreving.sak.enhet),
        soeknadFormat = null,
        vedtakLoependeTom = null,
        aktorId = statistikkTilbakekreving.tilbakekreving.sak.ident,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        ansvarligBeslutter = statistikkTilbakekreving.tilbakekreving.tilbakekreving.kravgrunnlag.saksbehandler.value,
        ferdigbehandletTidspunkt =
            statistikkTilbakekreving.tidspunkt.takeIf {
                statistikkTilbakekreving.tilbakekreving.status == TilbakekrevingStatus.ATTESTERT
            },
        saksbehandler = statistikkTilbakekreving.tilbakekreving.tilbakekreving.kravgrunnlag.saksbehandler.value,
        relatertTil = null,
    )

    private fun mapTilbakekrevingResultat(tilbakekreving: Tilbakekreving): TilbakekrevingResultat? =
        TilbakekrevingResultat.hoyesteGradAvTilbakekreving(
            tilbakekreving.perioder.flatMap { periode ->
                periode.tilbakekrevingsbeloep.mapNotNull { beloep -> beloep.resultat }
            },
        )

    private fun klageTilSakRad(
        statistikkKlage: StatistikkKlage,
        tekniskTid: LocalDateTime,
        hendelse: KlageHendelseType,
    ) = SakRad(
        id = -1,
        referanseId = statistikkKlage.klage.id,
        sakId = statistikkKlage.klage.sak.id,
        mottattTidspunkt =
            statistikkKlage.klage.innkommendeDokument?.mottattDato?.let {
                Tidspunkt.ofNorskTidssone(
                    it,
                    LocalTime.NOON,
                )
            } ?: statistikkKlage.klage.opprettet,
        registrertTidspunkt = statistikkKlage.klage.opprettet,
        type = "KLAGE",
        status = klageStatus(statistikkKlage, hendelse),
        resultat = resultatKlage(statistikkKlage),
        saksbehandler = statistikkKlage.saksbehandler,
        ansvarligEnhet = statistikkKlage.klage.sak.enhet,
        ansvarligBeslutter =
            when (val utfall = statistikkKlage.klage.utfall) {
                null ->
                    statistikkKlage.klage.formkrav
                        ?.saksbehandler
                        ?.ident

                else -> utfall.saksbehandler.ident
            },
        aktorId = statistikkKlage.klage.sak.ident,
        tekniskTid = tekniskTid.toTidspunkt(),
        sakYtelse = statistikkKlage.klage.sak.sakType.name,
        avdoedeForeldre = null,
        sakYtelsesgruppe = null,
        revurderingAarsak = null,
        vedtakLoependeFom = null,
        vedtakTidspunkt = null,
        beregning = null,
        avkorting = null,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        ferdigbehandletTidspunkt =
            statistikkKlage.tidspunkt.takeIf {
                statistikkKlage.klage.status in listOf(KlageStatus.AVBRUTT, KlageStatus.FERDIGSTILT)
            },
        behandlingMetode = BehandlingMetode.MANUELL,
        datoFoersteUtbetaling = null,
        opprettetAv = null,
        pesysId = null,
        resultatBegrunnelse =
            statistikkKlage.klage.aarsakTilAvbrytelse
                ?.name
                ?.takeIf { statistikkKlage.klage.status == KlageStatus.AVBRUTT },
        sakUtland = statistikkKlage.utlandstilknytningType?.let { SakUtland.fraUtlandstilknytningType(it) },
        sakUtlandEnhet = SakUtland.fraEnhetsnummer(statistikkKlage.klage.sak.enhet),
        soeknadFormat = null,
        vedtakLoependeTom = null,
        relatertTil =
            statistikkKlage.klage.formkrav
                ?.formkrav
                ?.vedtaketKlagenGjelder
                ?.behandlingId,
    )

    private fun klageStatus(
        statistikkKlage: StatistikkKlage,
        hendelse: KlageHendelseType,
    ): String {
        if (statistikkKlage.klage.status == KlageStatus.FERDIGSTILT) {
            return when (statistikkKlage.klage.utfall) {
                is KlageUtfallMedData.StadfesteVedtak, is KlageUtfallMedData.DelvisOmgjoering -> "OVERSENDT_KA"
                else -> hendelse.name
            }
        }
        return hendelse.name
    }

    private fun resultatKlage(statistikkKlage: StatistikkKlage): String? {
        if (statistikkKlage.klage.status == KlageStatus.AVBRUTT) {
            return "AVBRUTT"
        }
        return when (statistikkKlage.klage.utfall) {
            is KlageUtfallMedData.Omgjoering -> "OMGJOERING"
            is KlageUtfallMedData.DelvisOmgjoering -> "DELVIS_OMGJOERING"
            is KlageUtfallMedData.StadfesteVedtak -> "STADFESTE_VEDTAK"
            is KlageUtfallMedData.Avvist, is KlageUtfallMedData.AvvistMedOmgjoering -> "AVVIST"
            null -> null
        }
    }

    private fun behandlingTilSakRad(
        statistikkBehandling: StatistikkBehandling,
        behandlingHendelse: BehandlingHendelseType,
        tekniskTid: LocalDateTime,
    ): SakRad {
        val fellesRad =
            SakRad(
                id = -1,
                referanseId = statistikkBehandling.id,
                sakId = statistikkBehandling.sak.id,
                mottattTidspunkt = statistikkBehandling.behandlingOpprettet.toTidspunkt(),
                registrertTidspunkt = statistikkBehandling.behandlingOpprettet.toTidspunkt(),
                ferdigbehandletTidspunkt = null,
                vedtakTidspunkt = null,
                type = statistikkBehandling.behandlingType.name,
                status = behandlingHendelse.name,
                resultat = null,
                resultatBegrunnelse = null,
                behandlingMetode =
                    hentBehandlingMetode(
                        null,
                        statistikkBehandling.prosesstype,
                        statistikkBehandling.revurderingsaarsak,
                    ),
                opprettetAv = Vedtaksloesning.GJENNY.name,
                ansvarligBeslutter = null,
                aktorId = statistikkBehandling.sak.ident,
                datoFoersteUtbetaling = null,
                tekniskTid = tekniskTid.toTidspunkt(),
                sakYtelse = statistikkBehandling.sak.sakType.name,
                vedtakLoependeFom = null,
                vedtakLoependeTom = null,
                saksbehandler = null,
                ansvarligEnhet = statistikkBehandling.enhet,
                soeknadFormat = SoeknadFormat.DIGITAL,
                sakUtland =
                    SakUtland.fraUtlandstilknytning(statistikkBehandling.utlandstilknytning)
                        ?: SakUtland.fraEnhetsnummer(statistikkBehandling.sak.enhet),
                sakUtlandEnhet = SakUtland.fraEnhetsnummer(statistikkBehandling.sak.enhet),
                beregning = null,
                avkorting = null,
                sakYtelsesgruppe =
                    hentSakYtelsesgruppe(
                        statistikkBehandling.sak.sakType,
                        statistikkBehandling.avdoed ?: emptyList(),
                    ),
                avdoedeForeldre =
                    if (statistikkBehandling.sak.sakType == SakType.BARNEPENSJON) {
                        statistikkBehandling.avdoed
                    } else {
                        null
                    },
                revurderingAarsak = statistikkBehandling.revurderingsaarsak?.name,
                vedtaksloesning = statistikkBehandling.kilde,
                pesysId = statistikkBehandling.pesysId,
                relatertTil = null,
            )
        if (behandlingHendelse == BehandlingHendelseType.AVBRUTT) {
            return fellesRad.copy(
                ferdigbehandletTidspunkt = statistikkBehandling.sistEndret.toTidspunkt(),
                resultat = BehandlingResultat.AVBRUTT.name,
            )
        }
        return fellesRad
    }

    internal fun behandlingResultatFraVedtak(
        vedtak: VedtakDto,
        vedtakKafkaHendelseType: VedtakKafkaHendelseHendelseType,
        behandling: StatistikkBehandling,
    ): BehandlingResultat? {
        if (behandling.status == BehandlingStatus.AVBRUTT) {
            return BehandlingResultat.AVBRUTT
        }
        if (vedtakKafkaHendelseType !in
            listOf(
                VedtakKafkaHendelseHendelseType.ATTESTERT,
                VedtakKafkaHendelseHendelseType.IVERKSATT,
            )
        ) {
            return null
        }
        if (behandling.revurderingsaarsak == Revurderingaarsak.ETTEROPPGJOER) {
            return hentResultatFraEtteroppgjoerStatistikk(behandling)
        }
        return when (vedtak.type) {
            VedtakType.INNVILGELSE -> BehandlingResultat.INNVILGELSE
            VedtakType.OPPHOER -> BehandlingResultat.OPPHOER
            VedtakType.AVSLAG -> BehandlingResultat.AVSLAG
            VedtakType.ENDRING -> {
                when (
                    (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).utbetalingsperioder.any {
                        it.type == UtbetalingsperiodeType.OPPHOER
                    }
                ) {
                    true -> BehandlingResultat.OPPHOER
                    false -> BehandlingResultat.ENDRING
                }
            }

            VedtakType.TILBAKEKREVING,
            VedtakType.AVVIST_KLAGE,
            -> throw InternfeilException("Skal ikke mappe vedtak")
        }
    }

    private fun hentResultatFraEtteroppgjoerStatistikk(behandling: StatistikkBehandling): BehandlingResultat? {
        val etteroppgjoerRad = etteroppgjoerService.hentNyesteRad(behandling.relatertBehandlingId!!.toUUID())
        val resultat =
            krevIkkeNull(etteroppgjoerRad?.resultatType) {
                "Kan ikke sette resultat når det mangler i etteroppgjørsstatistikken"
            }
        return when (resultat) {
            EtteroppgjoerResultatType.TILBAKEKREVING -> BehandlingResultat.ETTEROPPGJOER_TILBAKEKREVING
            EtteroppgjoerResultatType.ETTERBETALING -> BehandlingResultat.ETTEROPPGJOER_ETTERBETALING
            EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING -> BehandlingResultat.ETTEROPPGJOER_INGEN_ENDRING_MED_UTBETALING
            EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING -> BehandlingResultat.ETTEROPPGJOER_INGEN_ENDRING_UTEN_UTBETALING
        }
    }
}

internal fun hentSakYtelsesgruppe(
    sakType: SakType,
    avdoede: List<String>,
): SakYtelsesgruppe? =
    when (sakType to avdoede.size) {
        SakType.BARNEPENSJON to 1 -> SakYtelsesgruppe.EN_AVDOED_FORELDER
        SakType.BARNEPENSJON to 2 -> SakYtelsesgruppe.FORELDRELOES
        else -> null
    }

internal fun hentBehandlingMetode(
    attestasjon: Attestasjon?,
    prosesstype: Prosesstype,
    revurderingAarsak: Revurderingaarsak?,
): BehandlingMetode =
    when (prosesstype) {
        Prosesstype.MANUELL ->
            if (attestasjon != null) {
                BehandlingMetode.TOTRINN
            } else {
                BehandlingMetode.MANUELL
            }

        Prosesstype.AUTOMATISK ->
            if (revurderingAarsak == Revurderingaarsak.REGULERING) {
                BehandlingMetode.AUTOMATISK_REGULERING
            } else {
                BehandlingMetode.AUTOMATISK
            }
    }
