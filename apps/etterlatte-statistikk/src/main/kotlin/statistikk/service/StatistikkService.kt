package no.nav.etterlatte.statistikk.service

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.database.KjoertStatus
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.BehandlingResultat
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.MaanedStatistikk
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SoeknadFormat
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.river.BehandlingHendelse
import no.nav.etterlatte.statistikk.river.BehandlingIntern
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

class StatistikkService(
    private val stoenadRepository: StoenadRepository,
    private val sakRepository: SakRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregningKlient: BeregningKlient
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun registrerStatistikkForVedtak(
        vedtakDto: VedtakDto,
        vedtakHendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtakDto, vedtakHendelse, tekniskTid)
        if (vedtakHendelse == VedtakHendelse.IVERKSATT) {
            val stoenadRad = when (vedtakDto.type) {
                VedtakType.INNVILGELSE -> stoenadRepository.lagreStoenadsrad(
                    vedtakTilStoenadsrad(vedtakDto, tekniskTid)
                )
                VedtakType.AVSLAG -> null
                VedtakType.ENDRING -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtakDto, tekniskTid))
                VedtakType.OPPHOER -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtakDto, tekniskTid))
            }
            return sakRad to stoenadRad
        }
        return sakRad to null
    }

    fun produserStoenadStatistikkForMaaned(maaned: YearMonth): MaanedStatistikk {
        val vedtak = stoenadRepository.hentRaderInnenforMaaned(maaned)
        return MaanedStatistikk(maaned, vedtak)
    }

    private fun registrerSakStatistikkForVedtak(
        vedtakDto: VedtakDto,
        hendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): SakRad? {
        return vedtakshendelseTilSakRad(vedtakDto, hendelse, tekniskTid).let { sakRad ->
            sakRepository.lagreRad(sakRad)
        }
    }

    private fun hentBeregningForBehandling(behandlingId: UUID): Beregning {
        return runBlocking {
            beregningKlient.hentBeregningForBehandling(behandlingId)
        }
    }

    private fun vedtakshendelseTilSakRad(
        vedtakDto: VedtakDto,
        hendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): SakRad {
        val detaljertBehandling = hentDetaljertBehandling(vedtakDto.behandling.id)
        val mottattTid = detaljertBehandling.soeknadMottattDato ?: detaljertBehandling.behandlingOpprettet
        val beregning = if (hendelse in listOf(
                VedtakHendelse.FATTET,
                VedtakHendelse.ATTESTERT,
                VedtakHendelse.IVERKSATT
            )
        ) {
            hentBeregningForBehandling(detaljertBehandling.id)
        } else {
            null
        }

        val foersteUtbetaling = if (vedtakDto.type == VedtakType.INNVILGELSE) {
            vedtakDto.vedtakFattet?.let {
                vedtakDto.utbetalingsperioder?.minByOrNull { it.periode.fom }
            }
        } else {
            null
        }

        return SakRad(
            id = -1,
            behandlingId = vedtakDto.behandling.id,
            sakId = vedtakDto.sak.id,
            mottattTidspunkt = mottattTid.toTidspunkt(),
            registrertTidspunkt = detaljertBehandling.behandlingOpprettet.toTidspunkt(),
            ferdigbehandletTidspunkt = vedtakDto.attestasjon?.tidspunkt?.toTidspunkt(),
            vedtakTidspunkt = vedtakDto.attestasjon?.tidspunkt?.toTidspunkt(),
            behandlingType = vedtakDto.behandling.type,
            behandlingStatus = hendelse.name,
            behandlingResultat = behandlingResultatFraVedtak(vedtakDto, hendelse, detaljertBehandling),
            resultatBegrunnelse = null,
            behandlingMetode = BehandlingMetode.MANUELL,
            soeknadFormat = SoeknadFormat.DIGITAL,
            opprettetAv = null,
            ansvarligBeslutter = vedtakDto.attestasjon?.attestant,
            aktorId = vedtakDto.sak.ident,
            datoFoersteUtbetaling = foersteUtbetaling?.periode?.fom?.atDay(1),
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtakDto.sak.sakType.name,
            vedtakLoependeFom = vedtakDto.virkningstidspunkt.atDay(1),
            vedtakLoependeTom = vedtakDto.virkningstidspunkt.atEndOfMonth(),
            saksbehandler = vedtakDto.vedtakFattet?.ansvarligSaksbehandler,
            ansvarligEnhet = vedtakDto.attestasjon?.attesterendeEnhet,
            sakUtland = SakUtland.NASJONAL,
            beregning = beregning
        )
    }

    private fun behandlingResultatFraVedtak(
        vedtakDto: VedtakDto,
        vedtakHendelse: VedtakHendelse,
        detaljertBehandling: DetaljertBehandling
    ): BehandlingResultat? {
        if (detaljertBehandling.status == BehandlingStatus.AVBRUTT) {
            return BehandlingResultat.AVBRUTT
        }
        if (vedtakHendelse !in listOf(VedtakHendelse.ATTESTERT, VedtakHendelse.IVERKSATT)) {
            return null
        }
        return when (vedtakDto.utbetalingsperioder?.any { it.type == UtbetalingsperiodeType.OPPHOER }) {
            true -> BehandlingResultat.OPPHOER
            false -> BehandlingResultat.VEDTAK
            null -> null
        }
    }

    private fun hentDetaljertBehandling(behandlingId: UUID) = runBlocking {
        behandlingKlient.hentDetaljertBehandling(behandlingId)
    }

    private fun hentPersongalleri(behandlingId: UUID): Persongalleri = runBlocking {
        behandlingKlient.hentPersongalleri(behandlingId)
    }

    private fun hentSak(sakId: Long) = runBlocking {
        behandlingKlient.hentSak(sakId)
    }

    private fun vedtakTilStoenadsrad(vedtakDto: VedtakDto, tekniskTid: LocalDateTime): StoenadRad {
        val persongalleri = hentPersongalleri(behandlingId = vedtakDto.behandling.id)
        val beregning = hentBeregningForBehandling(vedtakDto.behandling.id)
        return StoenadRad(
            id = -1,
            fnrSoeker = vedtakDto.sak.ident,
            fnrForeldre = persongalleri.avdoed,
            fnrSoesken = persongalleri.soesken,
            anvendtTrygdetid = "40",
            nettoYtelse = vedtakDto.utbetalingsperioder.firstOrNull()?.beloep.toString(),
            beregningType = "FOLKETRYGD",
            anvendtSats = "",
            behandlingId = vedtakDto.behandling.id,
            sakId = vedtakDto.sak.id,
            sakNummer = vedtakDto.sak.id,
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtakDto.sak.sakType.toString(),
            versjon = "",
            saksbehandler = vedtakDto.vedtakFattet!!.ansvarligSaksbehandler,
            attestant = vedtakDto.attestasjon?.attestant,
            vedtakLoependeFom = vedtakDto.virkningstidspunkt.atDay(1),
            vedtakLoependeTom = vedtakDto.virkningstidspunkt.atEndOfMonth(),
            beregning = beregning,
            vedtakType = vedtakDto.type
        )
    }

    private fun behandlingTilSakRad(
        behandlingIntern: BehandlingIntern,
        behandlingHendelse: BehandlingHendelse,
        tekniskTid: LocalDateTime
    ): SakRad {
        val sak = hentSak(behandlingIntern.sakId)
        val fellesRad = SakRad(
            id = -1,
            behandlingId = behandlingIntern.id,
            sakId = behandlingIntern.sakId,
            mottattTidspunkt = behandlingIntern.behandlingOpprettet.toTidspunkt(),
            registrertTidspunkt = behandlingIntern.behandlingOpprettet.toTidspunkt(),
            ferdigbehandletTidspunkt = null,
            vedtakTidspunkt = null,
            behandlingType = behandlingIntern.type,
            behandlingStatus = behandlingHendelse.name,
            behandlingResultat = null,
            resultatBegrunnelse = null,
            behandlingMetode = null,
            opprettetAv = null,
            ansvarligBeslutter = null,
            aktorId = behandlingIntern.persongalleri.soeker,
            datoFoersteUtbetaling = null,
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = sak.sakType.name,
            vedtakLoependeFom = null,
            vedtakLoependeTom = null,
            saksbehandler = null,
            ansvarligEnhet = null,
            soeknadFormat = SoeknadFormat.DIGITAL,
            sakUtland = SakUtland.NASJONAL,
            beregning = null
        )
        if (behandlingHendelse == BehandlingHendelse.AVBRUTT) {
            return fellesRad.copy(
                ferdigbehandletTidspunkt = behandlingIntern.sistEndret.toTidspunkt(),
                behandlingResultat = BehandlingResultat.AVBRUTT
            )
        }
        return fellesRad
    }

    fun registrerStatistikkForBehandlinghendelse(
        behandlingIntern: BehandlingIntern,
        hendelse: BehandlingHendelse,
        tekniskTid: LocalDateTime
    ): SakRad? {
        return sakRepository.lagreRad(behandlingTilSakRad(behandlingIntern, hendelse, tekniskTid))
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
            raderRegistrert
        )
    }
}

enum class VedtakHendelse {
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    IVERKSATT
}