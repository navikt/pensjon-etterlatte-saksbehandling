package no.nav.etterlatte.statistikk.service

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlient
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
import no.nav.etterlatte.statistikk.river.BehandlingHendelse
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class StatistikkService(
    private val stoenadRepository: StoenadRepository,
    private val sakRepository: SakRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregningKlient: BeregningKlient,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun registrerStatistikkForVedtak(
        vedtak: VedtakDto,
        vedtakKafkaHendelseType: VedtakKafkaHendelseType,
        tekniskTid: LocalDateTime,
    ): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtak, vedtakKafkaHendelseType, tekniskTid) ?: throw Exception("")
        if (vedtakKafkaHendelseType == VedtakKafkaHendelseType.IVERKSATT) {
            val stoenadRad =
                when (vedtak.type) {
                    VedtakType.INNVILGELSE,
                    VedtakType.ENDRING,
                    VedtakType.OPPHOER,
                    ->
                        stoenadRepository.lagreStoenadsrad(
                            vedtakTilStoenadsrad(
                                vedtak,
                                tekniskTid,
                                sakRad.kilde,
                                sakRad.pesysId,
                            ),
                        )
                    VedtakType.TILBAKEKREVING,
                    VedtakType.AVSLAG,
                    -> null
                }
            return sakRad to stoenadRad
        }
        return sakRad to null
    }

    fun produserStoenadStatistikkForMaaned(maaned: YearMonth): MaanedStatistikk {
        val vedtak = stoenadRepository.hentStoenadRaderInnenforMaaned(maaned)
        return MaanedStatistikk(maaned, vedtak)
    }

    private fun registrerSakStatistikkForVedtak(
        vedtak: VedtakDto,
        hendelse: VedtakKafkaHendelseType,
        tekniskTid: LocalDateTime,
    ): SakRad? {
        return vedtakshendelseTilSakRad(vedtak, hendelse, tekniskTid).let { sakRad ->
            sakRepository.lagreRad(sakRad)
        }
    }

    private fun hentBeregningForBehandling(behandlingId: UUID): Beregning? {
        return runBlocking {
            beregningKlient.hentBeregningForBehandling(behandlingId)
        }
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
        hendelse: VedtakKafkaHendelseType,
        tekniskTid: LocalDateTime,
    ): SakRad {
        val vedtakInnhold = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto)
        val statistikkBehandling = hentStatistikkBehandling(vedtak.behandlingId)
        val mottattTid = statistikkBehandling.soeknadMottattDato ?: statistikkBehandling.behandlingOpprettet
        val (beregning, avkorting) =
            when (hendelse) {
                VedtakKafkaHendelseType.FATTET,
                VedtakKafkaHendelseType.ATTESTERT,
                VedtakKafkaHendelseType.IVERKSATT,
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
            sakUtland = SakUtland.NASJONAL,
            beregning = beregning,
            avkorting = avkorting,
            sakYtelsesgruppe =
                hentSakYtelsesgruppe(
                    statistikkBehandling.sak.sakType,
                    statistikkBehandling.avdoed ?: emptyList(),
                ),
            avdoedeForeldre = statistikkBehandling.avdoed,
            revurderingAarsak = statistikkBehandling.revurderingsaarsak?.name,
            kilde = statistikkBehandling.kilde,
            pesysId = statistikkBehandling.pesysId,
        )
    }

    private fun behandlingResultatFraVedtak(
        vedtak: VedtakDto,
        vedtakKafkaHendelseType: VedtakKafkaHendelseType,
        statistikkBehandling: StatistikkBehandling,
    ): BehandlingResultat? {
        if (statistikkBehandling.status == BehandlingStatus.AVBRUTT) {
            return BehandlingResultat.AVBRUTT
        }
        if (vedtakKafkaHendelseType !in listOf(VedtakKafkaHendelseType.ATTESTERT, VedtakKafkaHendelseType.IVERKSATT)) {
            return null
        }
        return when (
            (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).utbetalingsperioder.any {
                it.type == UtbetalingsperiodeType.OPPHOER
            }
        ) {
            true -> BehandlingResultat.OPPHOER
            false -> BehandlingResultat.INNVILGELSE
        }
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
        val utbetalingsdato =
            vedtak.vedtakFattet?.tidspunkt?.let {
                val vedtattDato = it.toLocalDate()
                YearMonth.of(vedtattDato.year, vedtattDato.monthValue).plusMonths(1).atDay(20)
            }
        val vedtakInnhold = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto)
        return StoenadRad(
            id = -1,
            fnrSoeker = vedtak.sak.ident,
            fnrForeldre = persongalleri.avdoed,
            fnrSoesken = persongalleri.soesken,
            anvendtTrygdetid = "40",
            // TODO  Bør utbedres?
            nettoYtelse = vedtakInnhold.utbetalingsperioder.firstOrNull()?.beloep.toString(),
            beregningType = "FOLKETRYGD",
            anvendtSats = "",
            behandlingId = vedtakInnhold.behandling.id,
            sakId = vedtak.sak.id,
            sakNummer = vedtak.sak.id,
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
            sakUtland = SakUtland.NASJONAL,
            virkningstidspunkt = vedtakInnhold.virkningstidspunkt,
            utbetalingsdato = utbetalingsdato,
            kilde = vedtaksloesning,
            pesysId = pesysid,
        )
    }

    private fun klageTilSakRad(
        statistikkKlage: StatistikkKlage,
        tekniskTid: LocalDateTime,
        hendelse: KlageHendelseType,
    ) = SakRad(
        id = -1,
        referanseId = statistikkKlage.klage.id,
        sakId = statistikkKlage.klage.sak.id,
        mottattTidspunkt = statistikkKlage.klage.opprettet,
        registrertTidspunkt = statistikkKlage.tidspunkt,
        type = hendelse.name,
        status = statistikkKlage.klage.status.name,
        resultat = statistikkKlage.klage.kabalResultat?.name,
        saksbehandler = statistikkKlage.saksbehandler,
        ansvarligEnhet = statistikkKlage.klage.sak.enhet,
        ansvarligBeslutter = statistikkKlage.klage.formkrav?.saksbehandler?.ident,
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
        kilde = "BRUKER".takeIf(statistikkKlage.klage.innkommendeDokument?.innsender != null) ?: VedtaksLoesning.GJENNY 
        ferdigbehandletTidspunkt = null,
        behandlingMetode = null,
        datoFoersteUtbetaling = null,
        opprettetAv = null,
        pesysId = null,
        resultatBegrunnelse = null,
        sakUtland = null,
        soeknadFormat = null,
        vedtakLoependeTom = null,
    )

    private fun behandlingTilSakRad(
        statistikkBehandling: StatistikkBehandling,
        behandlingHendelse: BehandlingHendelse,
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
                sakUtland = SakUtland.NASJONAL,
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
                kilde = statistikkBehandling.kilde,
                pesysId = statistikkBehandling.pesysId,
            )
        if (behandlingHendelse == BehandlingHendelse.AVBRUTT) {
            return fellesRad.copy(
                ferdigbehandletTidspunkt = statistikkBehandling.sistEndret.toTidspunkt(),
                resultat = BehandlingResultat.AVBRUTT.name,
            )
        }
        return fellesRad
    }

    fun registrerStatistikkForBehandlinghendelse(
        statistikkBehandling: StatistikkBehandling,
        hendelse: BehandlingHendelse,
        tekniskTid: LocalDateTime,
    ): SakRad? {
        return sakRepository.lagreRad(behandlingTilSakRad(statistikkBehandling, hendelse, tekniskTid))
    }

    fun registrerStatistikkForKlagehendelse(
        statistikkKlage: StatistikkKlage,
        tekniskTid: LocalDateTime,
        hendelse: KlageHendelseType,
    ): SakRad? {
        return sakRepository.lagreRad(klageTilSakRad(statistikkKlage, tekniskTid, hendelse))
    }

    fun statistikkProdusertForMaaned(maaned: YearMonth): KjoertStatus {
        return stoenadRepository.kjoertStatusForMaanedsstatistikk(maaned)
    }

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
