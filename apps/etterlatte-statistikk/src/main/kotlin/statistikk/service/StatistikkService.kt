package no.nav.etterlatte.statistikk.service

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class StatistikkService(
    private val stoenadRepository: StatistikkRepository,
    private val sakRepository: SakstatistikkRepository,
    private val behandlingClient: BehandlingClient
) : VedlikeholdService {

    fun registrerStatistikkForVedtak(vedtak: Vedtak, vedtakHendelse: VedtakHendelse): Pair<SakRad?, StoenadRad?> {
        val sakRad = registrerSakStatistikkForVedtak(vedtak, vedtakHendelse)
        if (vedtakHendelse == VedtakHendelse.ATTESTERT) {
            val stoenadRad = when (vedtak.type) {
                VedtakType.INNVILGELSE -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
                VedtakType.AVSLAG -> null
                VedtakType.ENDRING -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
                VedtakType.OPPHOER -> stoenadRepository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
            }
            return sakRad to stoenadRad
        }
        return sakRad to null
    }

    private fun registrerSakStatistikkForVedtak(vedtak: Vedtak, hendelse: VedtakHendelse): SakRad? {
        return vedtakshendelseTilSakRad(vedtak, hendelse).let { sakRad ->
            sakRepository.lagreRad(sakRad)
        }
    }

    private fun behandlingResultatFraVedtak(
        vedtak: Vedtak,
        detaljertBehandling: DetaljertBehandling
    ): BehandlingResultat? {
        if (vedtak.vedtakFattet != null) {
            return BehandlingResultat.VEDTAK
        }
        if (detaljertBehandling.status == BehandlingStatus.AVBRUTT) {
            return BehandlingResultat.AVBRUTT
        }
        return null
    }

    private fun vedtakshendelseTilSakRad(vedtak: Vedtak, hendelse: VedtakHendelse): SakRad {
        val detaljertBehandling = hentDetaljertBehandling(vedtak.behandling.id)
        val mottattTid = detaljertBehandling.soeknadMottattDato ?: detaljertBehandling.behandlingOpprettet

        val fellesRad = SakRad(
            id = -1,
            behandlingId = vedtak.behandling.id,
            sakId = vedtak.sak.id,
            mottattTidspunkt = mottattTid.toTidspunkt(ZoneOffset.UTC),
            registrertTidspunkt = detaljertBehandling.behandlingOpprettet.toTidspunkt(ZoneOffset.UTC),
            ferdigbehandletTidspunkt = vedtak.vedtakFattet?.tidspunkt?.toTidspunkt(),
            vedtakTidspunkt = vedtak.vedtakFattet?.tidspunkt?.toTidspunkt(),
            behandlingType = vedtak.behandling.type,
            behandlingStatus = hendelse.name,
            behandlingResultat = behandlingResultatFraVedtak(vedtak, detaljertBehandling),
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
            tekniskTid = detaljertBehandling.sistEndret.toTidspunkt(ZoneOffset.UTC),
            sakYtelse = vedtak.sak.sakType,
            vedtakLoependeFom = vedtak.virk.fom.atDay(1),
            vedtakLoependeTom = vedtak.virk.tom?.atEndOfMonth(),
            saksbehandler = vedtak.vedtakFattet?.ansvarligSaksbehandler,
            ansvarligEnhet = vedtak.vedtakFattet?.ansvarligEnhet,
            sakUtland = SakUtland.NASJONAL
        )
        if (hendelse == VedtakHendelse.IVERKSATT || hendelse == VedtakHendelse.ATTESTERT) {
            return fellesRad.copy(
                behandlingResultat = behandlingResultatFraVedtak(vedtak)
            )
        }

        return fellesRad
    }

    private fun behandlingResultatFraVedtak(vedtak: Vedtak): BehandlingResultat? {
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

    private fun vedtakTilStoenadsrad(vedtak: Vedtak): StoenadRad {
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
            Instant.now(),
            vedtak.sak.sakType.toString(),
            "",
            vedtak.vedtakFattet!!.ansvarligSaksbehandler,
            vedtak.attestasjon?.attestant,
            vedtak.virk.fom.atDay(1),
            vedtak.virk.tom?.atEndOfMonth()
        )
    }

    override fun slettSak(sakId: Long) {
        stoenadRepository.slettSak(sakId)
        sakRepository.slettSak(sakId)
    }

    private fun behandlingTilSakRad(behandling: Behandling, behandlingHendelse: BehandlingHendelse): SakRad {
        val sak = hentSak(behandling.sak)
        val fellesRad = SakRad(
            id = -1,
            behandlingId = behandling.id,
            sakId = behandling.sak,
            mottattTidspunkt = Tidspunkt(instant = behandling.behandlingOpprettet.toInstant(ZoneOffset.UTC)),
            registrertTidspunkt = Tidspunkt(instant = behandling.behandlingOpprettet.toInstant(ZoneOffset.UTC)),
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
            tekniskTid = Tidspunkt(instant = behandling.sistEndret.toInstant(ZoneOffset.UTC)),
            sakYtelse = sak.sakType,
            vedtakLoependeFom = null,
            vedtakLoependeTom = null,
            saksbehandler = null,
            ansvarligEnhet = null,
            soeknadFormat = SoeknadFormat.DIGITAL,
            sakUtland = SakUtland.NASJONAL
        )
        if (behandlingHendelse == BehandlingHendelse.AVBRUTT) {
            return fellesRad.copy(
                ferdigbehandletTidspunkt = Tidspunkt(instant = behandling.sistEndret.toInstant(ZoneOffset.UTC)),
                behandlingResultat = BehandlingResultat.AVBRUTT
            )
        }
        return fellesRad
    }

    fun registrerStatistikkForBehandlinghendelse(behandling: Behandling, hendelse: BehandlingHendelse): SakRad? {
        return sakRepository.lagreRad(behandlingTilSakRad(behandling, hendelse))
    }
}

enum class VedtakHendelse {
    FATTET,
    ATTESTERT,
    UNDERKJENT,
    IVERKSATT
}