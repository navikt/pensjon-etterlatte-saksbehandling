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
import java.util.UUID

class StatistikkService(
    private val stoenadRepository: StoenadRepository,
    private val sakRepository: SakRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregningKlient: BeregningKlient
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun registrerStatistikkForVedtak(
        vedtak: VedtakDto,
        vedtakHendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtak, vedtakHendelse, tekniskTid)
        if (vedtakHendelse == VedtakHendelse.IVERKSATT) {
            val stoenadRad = when (vedtak.type) {
                VedtakType.INNVILGELSE -> stoenadRepository.lagreStoenadsrad(
                    vedtakTilStoenadsrad(vedtak, tekniskTid)
                )
                VedtakType.AVSLAG -> null
                VedtakType.ENDRING -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak, tekniskTid))
                VedtakType.OPPHOER -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak, tekniskTid))
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
        vedtak: VedtakDto,
        hendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
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

    private fun vedtakshendelseTilSakRad(
        vedtak: VedtakDto,
        hendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): SakRad {
        val detaljertBehandling = hentDetaljertBehandling(vedtak.behandling.id)
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

        val foersteUtbetaling = if (vedtak.type == VedtakType.INNVILGELSE) {
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
            behandlingMetode = BehandlingMetode.MANUELL,
            soeknadFormat = SoeknadFormat.DIGITAL,
            opprettetAv = null,
            ansvarligBeslutter = vedtak.attestasjon?.attestant,
            aktorId = vedtak.sak.ident,
            datoFoersteUtbetaling = foersteUtbetaling?.periode?.fom?.atDay(1),
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtak.sak.sakType.name,
            vedtakLoependeFom = vedtak.virkningstidspunkt.atDay(1),
            vedtakLoependeTom = vedtak.virkningstidspunkt.atEndOfMonth(),
            saksbehandler = vedtak.vedtakFattet?.ansvarligSaksbehandler,
            ansvarligEnhet = vedtak.attestasjon?.attesterendeEnhet,
            sakUtland = SakUtland.NASJONAL,
            beregning = beregning
        )
    }

    private fun behandlingResultatFraVedtak(
        vedtak: VedtakDto,
        vedtakHendelse: VedtakHendelse,
        detaljertBehandling: DetaljertBehandling
    ): BehandlingResultat? {
        if (detaljertBehandling.status == BehandlingStatus.AVBRUTT) {
            return BehandlingResultat.AVBRUTT
        }
        if (vedtakHendelse !in listOf(VedtakHendelse.ATTESTERT, VedtakHendelse.IVERKSATT)) {
            return null
        }
        return when (vedtak.utbetalingsperioder.any { it.type == UtbetalingsperiodeType.OPPHOER }) {
            true -> BehandlingResultat.OPPHOER
            false -> BehandlingResultat.VEDTAK
        }
    }

    private fun hentDetaljertBehandling(behandlingId: UUID) = runBlocking {
        behandlingKlient.hentDetaljertBehandling(behandlingId)
    }

    private fun hentPersongalleri(behandlingId: UUID): Persongalleri = runBlocking {
        behandlingKlient.hentPersongalleri(behandlingId)
    }

    private fun vedtakTilStoenadsrad(vedtak: VedtakDto, tekniskTid: LocalDateTime): StoenadRad {
        val persongalleri = hentPersongalleri(behandlingId = vedtak.behandling.id)
        val beregning = hentBeregningForBehandling(vedtak.behandling.id)
        return StoenadRad(
            id = -1,
            fnrSoeker = vedtak.sak.ident,
            fnrForeldre = persongalleri.avdoed,
            fnrSoesken = persongalleri.soesken,
            anvendtTrygdetid = "40",
            nettoYtelse = vedtak.utbetalingsperioder.firstOrNull()?.beloep.toString(),
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
            vedtakType = vedtak.type
        )
    }

    private fun behandlingTilSakRad(
        behandlingIntern: BehandlingIntern,
        behandlingHendelse: BehandlingHendelse,
        tekniskTid: LocalDateTime
    ): SakRad {
        val fellesRad = SakRad(
            id = -1,
            behandlingId = behandlingIntern.id,
            sakId = behandlingIntern.sak.id,
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
            aktorId = behandlingIntern.sak.ident,
            datoFoersteUtbetaling = null,
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = behandlingIntern.sak.sakType.name,
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