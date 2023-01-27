package no.nav.etterlatte.statistikk.service

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.clients.BehandlingClient
import no.nav.etterlatte.statistikk.database.SakstatistikkRepository
import no.nav.etterlatte.statistikk.database.StatistikkRepository
import no.nav.etterlatte.statistikk.database.StoenadRad
import no.nav.etterlatte.statistikk.river.Behandling
import no.nav.etterlatte.statistikk.river.BehandlingHendelse
import no.nav.etterlatte.statistikk.sak.BehandlingMetode
import no.nav.etterlatte.statistikk.sak.BehandlingResultat
import no.nav.etterlatte.statistikk.sak.SakRad
import no.nav.etterlatte.statistikk.sak.SakUtland
import no.nav.etterlatte.statistikk.sak.SoeknadFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class StatistikkService(
    private val stoenadRepository: StatistikkRepository,
    private val sakRepository: SakstatistikkRepository,
    private val behandlingClient: BehandlingClient
) {

    fun registrerStatistikkForVedtak(
        vedtak: Vedtak,
        vedtakHendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtak, vedtakHendelse, tekniskTid)
        if (vedtakHendelse == VedtakHendelse.ATTESTERT) {
            val stoenadRad = when (vedtak.type) {
                VedtakType.INNVILGELSE -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak, tekniskTid))
                VedtakType.AVSLAG -> null
                VedtakType.ENDRING -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak, tekniskTid))
                VedtakType.OPPHOER -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak, tekniskTid))
            }
            return sakRad to stoenadRad
        }
        return sakRad to null
    }

    private fun registrerSakStatistikkForVedtak(
        vedtak: Vedtak,
        hendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): SakRad? {
        return vedtakshendelseTilSakRad(vedtak, hendelse, tekniskTid).let { sakRad ->
            sakRepository.lagreRad(sakRad)
        }
    }

    private fun vedtakshendelseTilSakRad(vedtak: Vedtak, hendelse: VedtakHendelse, tekniskTid: LocalDateTime): SakRad {
        val detaljertBehandling = hentDetaljertBehandling(vedtak.behandling.id)
        val mottattTid = detaljertBehandling.soeknadMottattDato ?: detaljertBehandling.behandlingOpprettet

        val fellesRad = SakRad(
            id = -1,
            behandlingId = vedtak.behandling.id,
            sakId = vedtak.sak.id,
            mottattTidspunkt = mottattTid.toTidspunkt(ZoneOffset.UTC),
            registrertTidspunkt = detaljertBehandling.behandlingOpprettet.toTidspunkt(ZoneOffset.UTC),
            ferdigbehandletTidspunkt = vedtak.attestasjon?.tidspunkt?.toTidspunkt(),
            vedtakTidspunkt = vedtak.attestasjon?.tidspunkt?.toTidspunkt(),
            behandlingType = vedtak.behandling.type,
            behandlingStatus = hendelse.name,
            behandlingResultat = behandlingResultatFraVedtak(vedtak, hendelse, detaljertBehandling),
            resultatBegrunnelse = null,
            behandlingMetode = BehandlingMetode.MANUELL,
            soeknadFormat = SoeknadFormat.DIGITAL,
            opprettetAv = null,
            ansvarligBeslutter = vedtak.attestasjon?.attestant,
            aktorId = vedtak.sak.ident,
            datoFoersteUtbetaling = vedtak.vedtakFattet?.let {
                vedtak.pensjonTilUtbetaling?.minByOrNull { it.periode.fom }?.periode?.fom?.atDay(
                    20
                )
            },
            tekniskTid = tekniskTid.toTidspunkt(ZoneOffset.UTC),
            sakYtelse = vedtak.sak.sakType.name,
            vedtakLoependeFom = vedtak.virk.fom.atDay(1),
            vedtakLoependeTom = vedtak.virk.tom?.atEndOfMonth(),
            saksbehandler = vedtak.vedtakFattet?.ansvarligSaksbehandler,
            ansvarligEnhet = vedtak.vedtakFattet?.ansvarligEnhet,
            sakUtland = SakUtland.NASJONAL
        )
        return fellesRad
    }

    private fun behandlingResultatFraVedtak(
        vedtak: Vedtak,
        vedtakHendelse: VedtakHendelse,
        detaljertBehandling: DetaljertBehandling
    ): BehandlingResultat? {
        if (detaljertBehandling.status == BehandlingStatus.AVBRUTT) {
            return BehandlingResultat.AVBRUTT
        }
        if (vedtakHendelse !in listOf(VedtakHendelse.ATTESTERT, VedtakHendelse.IVERKSATT)) {
            return null
        }
        return when (vedtak.pensjonTilUtbetaling?.any { it.type == UtbetalingsperiodeType.OPPHOER }) {
            true -> BehandlingResultat.OPPHOER
            false -> BehandlingResultat.VEDTAK
            null -> null
        }
    }

    private fun hentDetaljertBehandling(behandlingId: UUID) = runBlocking {
        behandlingClient.hentDetaljertBehandling(behandlingId)
    }

    private fun hentPersongalleri(behandlingId: UUID): Persongalleri = runBlocking {
        behandlingClient.hentPersongalleri(behandlingId)
    }

    private fun hentSak(sakId: Long) = runBlocking {
        behandlingClient.hentSak(sakId)
    }

    private fun vedtakTilStoenadsrad(vedtak: Vedtak, tekniskTid: LocalDateTime): StoenadRad {
        val persongalleri = hentPersongalleri(behandlingId = vedtak.behandling.id)
        return StoenadRad(
            -1,
            vedtak.sak.ident,
            persongalleri.avdoed,
            persongalleri.soesken,
            "40",
            vedtak.beregning?.sammendrag?.firstOrNull()?.beloep.toString(),
            "FOLKETRYGD",
            "",
            vedtak.behandling.id,
            vedtak.sak.id,
            vedtak.sak.id,
            tekniskTid.toTidspunkt(ZoneOffset.UTC).instant,
            vedtak.sak.sakType.toString(),
            "",
            vedtak.vedtakFattet!!.ansvarligSaksbehandler,
            vedtak.attestasjon?.attestant,
            vedtak.virk.fom.atDay(1),
            vedtak.virk.tom?.atEndOfMonth()
        )
    }

    private fun behandlingTilSakRad(
        behandling: Behandling,
        behandlingHendelse: BehandlingHendelse,
        tekniskTid: LocalDateTime
    ): SakRad {
        val sak = hentSak(behandling.sak)
        val fellesRad = SakRad(
            id = -1,
            behandlingId = behandling.id,
            sakId = behandling.sak,
            mottattTidspunkt = behandling.behandlingOpprettet.toTidspunkt(ZoneOffset.UTC),
            registrertTidspunkt = behandling.behandlingOpprettet.toTidspunkt(ZoneOffset.UTC),
            ferdigbehandletTidspunkt = null,
            vedtakTidspunkt = null,
            behandlingType = behandling.type,
            behandlingStatus = behandlingHendelse.name,
            behandlingResultat = null,
            resultatBegrunnelse = null,
            behandlingMetode = null,
            opprettetAv = null,
            ansvarligBeslutter = null,
            aktorId = behandling.persongalleri.soeker,
            datoFoersteUtbetaling = null,
            tekniskTid = tekniskTid.toTidspunkt(ZoneOffset.UTC),
            sakYtelse = sak.sakType.name,
            vedtakLoependeFom = null,
            vedtakLoependeTom = null,
            saksbehandler = null,
            ansvarligEnhet = null,
            soeknadFormat = SoeknadFormat.DIGITAL,
            sakUtland = SakUtland.NASJONAL
        )
        if (behandlingHendelse == BehandlingHendelse.AVBRUTT) {
            return fellesRad.copy(
                ferdigbehandletTidspunkt = behandling.sistEndret.toTidspunkt(ZoneOffset.UTC),
                behandlingResultat = BehandlingResultat.AVBRUTT
            )
        }
        return fellesRad
    }

    fun registrerStatistikkForBehandlinghendelse(
        behandling: Behandling,
        hendelse: BehandlingHendelse,
        tekniskTid: LocalDateTime
    ): SakRad? {
        return sakRepository.lagreRad(behandlingTilSakRad(behandling, hendelse, tekniskTid))
    }
}

enum class VedtakHendelse {
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    IVERKSATT
}