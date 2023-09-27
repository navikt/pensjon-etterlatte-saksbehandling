package no.nav.etterlatte.statistikk.service

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.UTBETALINGSDAG
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
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
        vedtakHendelse: VedtakHendelse,
        tekniskTid: LocalDateTime,
    ): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtak, vedtakHendelse, tekniskTid)
        if (vedtakHendelse == VedtakHendelse.IVERKSATT) {
            val stoenadRad =
                when (vedtak.type) {
                    VedtakType.INNVILGELSE,
                    VedtakType.ENDRING,
                    VedtakType.OPPHOER,
                    -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak, tekniskTid))

                    VedtakType.AVSLAG -> null
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
        hendelse: VedtakHendelse,
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
                beregningKlient.hentAvkortingForBehandling(vedtak.behandling.id)
            }
        }
        return null
    }

    private fun vedtakshendelseTilSakRad(
        vedtak: VedtakDto,
        hendelse: VedtakHendelse,
        tekniskTid: LocalDateTime,
    ): SakRad {
        val detaljertBehandling = hentDetaljertBehandling(vedtak.behandling.id)
        val mottattTid = detaljertBehandling.soeknadMottattDato ?: detaljertBehandling.behandlingOpprettet
        val (beregning, avkorting) =
            when (hendelse) {
                VedtakHendelse.FATTET,
                VedtakHendelse.ATTESTERT,
                VedtakHendelse.IVERKSATT,
                ->
                    Pair(
                        hentBeregningForBehandling(detaljertBehandling.id),
                        hentAvkortingForBehandling(vedtak),
                    )
                else -> Pair(null, null)
            }

        val foersteUtbetaling =
            if (vedtak.type == VedtakType.INNVILGELSE) {
                vedtak.vedtakFattet?.let {
                    vedtak.utbetalingsperioder.minByOrNull { it.periode.fom }
                }
            } else {
                null
            }

        return SakRad(
            id = -1,
            behandlingId = vedtak.behandling.id,
            sakId = vedtak.sak.id,
            mottattTidspunkt = mottattTid.toTidspunkt(),
            registrertTidspunkt = detaljertBehandling.behandlingOpprettet.toTidspunkt(),
            ferdigbehandletTidspunkt = vedtak.attestasjon?.tidspunkt,
            vedtakTidspunkt = vedtak.attestasjon?.tidspunkt,
            behandlingType = vedtak.behandling.type,
            behandlingStatus = hendelse.name,
            behandlingResultat = behandlingResultatFraVedtak(vedtak, hendelse, detaljertBehandling),
            resultatBegrunnelse = null,
            behandlingMetode = detaljertBehandling.behandlingMetode(vedtak.attestasjon),
            soeknadFormat = SoeknadFormat.DIGITAL,
            opprettetAv = "GJENNY",
            ansvarligBeslutter = vedtak.attestasjon?.attestant,
            aktorId = vedtak.sak.ident,
            datoFoersteUtbetaling = foersteUtbetaling?.periode?.fom?.atDay(1),
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtak.sak.sakType.name,
            vedtakLoependeFom = vedtak.virkningstidspunkt.atDay(1),
            vedtakLoependeTom = vedtak.virkningstidspunkt.atEndOfMonth(),
            saksbehandler = vedtak.vedtakFattet?.ansvarligSaksbehandler,
            ansvarligEnhet = vedtak.attestasjon?.attesterendeEnhet ?: detaljertBehandling.enhet,
            sakUtland = SakUtland.NASJONAL,
            beregning = beregning,
            avkorting = avkorting,
            sakYtelsesgruppe = hentSakYtelsesgruppe(detaljertBehandling.sakType, detaljertBehandling.avdoed ?: emptyList()),
            avdoedeForeldre = detaljertBehandling.avdoed,
            revurderingAarsak = detaljertBehandling.revurderingsaarsak?.name,
        )
    }

    private fun behandlingResultatFraVedtak(
        vedtak: VedtakDto,
        vedtakHendelse: VedtakHendelse,
        detaljertBehandling: DetaljertBehandling,
    ): BehandlingResultat? {
        if (detaljertBehandling.status == BehandlingStatus.AVBRUTT) {
            return BehandlingResultat.AVBRUTT
        }
        if (vedtakHendelse !in listOf(VedtakHendelse.ATTESTERT, VedtakHendelse.IVERKSATT)) {
            return null
        }
        return when (vedtak.utbetalingsperioder.any { it.type == UtbetalingsperiodeType.OPPHOER }) {
            true -> BehandlingResultat.OPPHOER
            false -> BehandlingResultat.INNVILGELSE
        }
    }

    private fun hentDetaljertBehandling(behandlingId: UUID) =
        runBlocking {
            behandlingKlient.hentDetaljertBehandling(behandlingId)
        }

    private fun hentPersongalleri(behandlingId: UUID): Persongalleri =
        runBlocking {
            behandlingKlient.hentPersongalleri(behandlingId)
        }

    private fun vedtakTilStoenadsrad(
        vedtak: VedtakDto,
        tekniskTid: LocalDateTime,
    ): StoenadRad {
        val persongalleri = hentPersongalleri(behandlingId = vedtak.behandling.id)

        val beregning = hentBeregningForBehandling(vedtak.behandling.id)
        val avkorting = hentAvkortingForBehandling(vedtak)
        val utbetalingsdato =
            vedtak.vedtakFattet?.tidspunkt?.let {
                val vedtattDato = it.toLocalDate()
                YearMonth.of(vedtattDato.year, vedtattDato.monthValue).plusMonths(1).atDay(UTBETALINGSDAG)
            }
        return StoenadRad(
            id = -1,
            fnrSoeker = vedtak.sak.ident,
            fnrForeldre = persongalleri.avdoed,
            fnrSoesken = persongalleri.soesken,
            anvendtTrygdetid = "40",
            nettoYtelse = vedtak.utbetalingsperioder.firstOrNull()?.beloep.toString(), // TODO  Bør utbedres?
            beregningType = "FOLKETRYGD",
            anvendtSats = "",
            behandlingId = vedtak.behandling.id,
            sakId = vedtak.sak.id,
            sakNummer = vedtak.sak.id,
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtak.sak.sakType.toString(),
            versjon = "",
            saksbehandler = vedtak.vedtakFattet!!.ansvarligSaksbehandler,
            attestant = vedtak.attestasjon?.attestant,
            vedtakLoependeFom = vedtak.virkningstidspunkt.atDay(1),
            vedtakLoependeTom = null,
            beregning = beregning,
            avkorting = avkorting,
            vedtakType = vedtak.type,
            sakUtland = SakUtland.NASJONAL,
            virkningstidspunkt = vedtak.virkningstidspunkt,
            utbetalingsdato = utbetalingsdato,
        )
    }

    private fun behandlingTilSakRad(
        statistikkBehandling: StatistikkBehandling,
        behandlingHendelse: BehandlingHendelse,
        tekniskTid: LocalDateTime,
    ): SakRad {
        val fellesRad =
            SakRad(
                id = -1,
                behandlingId = statistikkBehandling.id,
                sakId = statistikkBehandling.sak.id,
                mottattTidspunkt = statistikkBehandling.behandlingOpprettet.toTidspunkt(),
                registrertTidspunkt = statistikkBehandling.behandlingOpprettet.toTidspunkt(),
                ferdigbehandletTidspunkt = null,
                vedtakTidspunkt = null,
                behandlingType = statistikkBehandling.behandlingType,
                behandlingStatus = behandlingHendelse.name,
                behandlingResultat = null,
                resultatBegrunnelse = null,
                behandlingMetode = statistikkBehandling.behandlingMetode(null),
                opprettetAv = "GJENNY",
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
                sakYtelsesgruppe = hentSakYtelsesgruppe(statistikkBehandling.sak.sakType, statistikkBehandling.avdoed ?: emptyList()),
                avdoedeForeldre =
                    if (statistikkBehandling.sak.sakType == SakType.BARNEPENSJON) {
                        statistikkBehandling.avdoed
                    } else {
                        null
                    },
                revurderingAarsak = statistikkBehandling.revurderingsaarsak?.name,
            )
        if (behandlingHendelse == BehandlingHendelse.AVBRUTT) {
            return fellesRad.copy(
                ferdigbehandletTidspunkt = statistikkBehandling.sistEndret.toTidspunkt(),
                behandlingResultat = BehandlingResultat.AVBRUTT,
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

enum class VedtakHendelse {
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    IVERKSATT,
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

internal fun StatistikkBehandling.behandlingMetode(attestasjon: Attestasjon?): BehandlingMetode =
    when (this.prosesstype) {
        Prosesstype.MANUELL ->
            if (attestasjon != null) {
                BehandlingMetode.TOTRINN
            } else {
                BehandlingMetode.MANUELL
            }

        Prosesstype.AUTOMATISK ->
            if (this.revurderingsaarsak == RevurderingAarsak.REGULERING) {
                BehandlingMetode.AUTOMATISK_REGULERING
            } else {
                BehandlingMetode.AUTOMATISK
            }
    }

internal fun DetaljertBehandling.behandlingMetode(attestasjon: Attestasjon?): BehandlingMetode =
    when (this.prosesstype) {
        Prosesstype.MANUELL ->
            if (attestasjon != null) {
                BehandlingMetode.TOTRINN
            } else {
                BehandlingMetode.MANUELL
            }

        Prosesstype.AUTOMATISK ->
            if (this.revurderingsaarsak == RevurderingAarsak.REGULERING) {
                BehandlingMetode.AUTOMATISK_REGULERING
            } else {
                BehandlingMetode.AUTOMATISK
            }
    }
