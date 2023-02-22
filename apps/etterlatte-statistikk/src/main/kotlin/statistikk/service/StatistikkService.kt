package no.nav.etterlatte.statistikk.service

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.statistikk.clients.BehandlingKlient
import no.nav.etterlatte.statistikk.clients.BeregningKlient
import no.nav.etterlatte.statistikk.database.SakRepository
import no.nav.etterlatte.statistikk.database.StoenadRepository
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.BehandlingResultat
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SoeknadFormat
import no.nav.etterlatte.statistikk.domain.StoenadRad
import no.nav.etterlatte.statistikk.river.Behandling
import no.nav.etterlatte.statistikk.river.BehandlingHendelse
import java.time.LocalDateTime
import java.util.*

class StatistikkService(
    private val stoenadRepository: StoenadRepository,
    private val sakRepository: SakRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregningKlient: BeregningKlient
) {

    fun registrerStatistikkForVedtak(
        vedtak: Vedtak,
        vedtakHendelse: VedtakHendelse,
        tekniskTid: LocalDateTime
    ): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtak, vedtakHendelse, tekniskTid)
        if (vedtakHendelse == VedtakHendelse.IVERKSATT) {
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

    private fun hentBeregningForBehandling(behandlingId: UUID): Beregning {
        return runBlocking {
            beregningKlient.hentBeregningForBehandling(behandlingId)
        }
    }

    private fun vedtakshendelseTilSakRad(vedtak: Vedtak, hendelse: VedtakHendelse, tekniskTid: LocalDateTime): SakRad {
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
                vedtak.pensjonTilUtbetaling?.minByOrNull { it.periode.fom }
            }
        } else {
            null
        }

        val fellesRad = SakRad(
            id = -1,
            behandlingId = vedtak.behandling.id,
            sakId = vedtak.sak.id,
            mottattTidspunkt = mottattTid.toTidspunkt(),
            registrertTidspunkt = detaljertBehandling.behandlingOpprettet.toTidspunkt(),
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
            datoFoersteUtbetaling = foersteUtbetaling?.periode?.fom?.atDay(1),
            tekniskTid = tekniskTid.toTidspunkt(),
            sakYtelse = vedtak.sak.sakType.name,
            vedtakLoependeFom = vedtak.virk.fom.atDay(1),
            vedtakLoependeTom = vedtak.virk.tom?.atEndOfMonth(),
            saksbehandler = vedtak.vedtakFattet?.ansvarligSaksbehandler,
            ansvarligEnhet = vedtak.attestasjon?.attesterendeEnhet,
            sakUtland = SakUtland.NASJONAL,
            beregning = beregning
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
        behandlingKlient.hentDetaljertBehandling(behandlingId)
    }

    private fun hentPersongalleri(behandlingId: UUID): Persongalleri = runBlocking {
        behandlingKlient.hentPersongalleri(behandlingId)
    }

    private fun hentSak(sakId: Long) = runBlocking {
        behandlingKlient.hentSak(sakId)
    }

    private fun vedtakTilStoenadsrad(vedtak: Vedtak, tekniskTid: LocalDateTime): StoenadRad {
        val persongalleri = hentPersongalleri(behandlingId = vedtak.behandling.id)
        val beregning = hentBeregningForBehandling(vedtak.behandling.id)
        return StoenadRad(
            id = -1,
            fnrSoeker = vedtak.sak.ident,
            fnrForeldre = persongalleri.avdoed,
            fnrSoesken = persongalleri.soesken,
            anvendtTrygdetid = "40",
            nettoYtelse = vedtak.beregning?.sammendrag?.firstOrNull()?.beloep.toString(),
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
            vedtakLoependeFom = vedtak.virk.fom.atDay(1),
            vedtakLoependeTom = vedtak.virk.tom?.atEndOfMonth(),
            beregning = beregning
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
            mottattTidspunkt = behandling.behandlingOpprettet.toTidspunkt(),
            registrertTidspunkt = behandling.behandlingOpprettet.toTidspunkt(),
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
                ferdigbehandletTidspunkt = behandling.sistEndret.toTidspunkt(),
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