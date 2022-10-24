package no.nav.etterlatte.statistikk.statistikk

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.statistikk.client.BehandlingClient
import no.nav.etterlatte.statistikk.database.StatistikkRepository
import no.nav.etterlatte.statistikk.database.StoenadRad
import rapidsandrivers.vedlikehold.VedlikeholdService
import java.time.Instant
import java.util.UUID

class StatistikkService(
    private val repository: StatistikkRepository,
    private val behandlingClient: BehandlingClient
) : VedlikeholdService {

    fun registrerStatistikkForVedtak(vedtak: Vedtak): StoenadRad? {
        kotlin.run { }
        return when (vedtak.type) {
            VedtakType.INNVILGELSE -> repository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
            VedtakType.AVSLAG -> null
            VedtakType.ENDRING -> repository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
            VedtakType.OPPHOER -> repository.lagreStoenadsrad(vedtakTilStoenadsrad(vedtak))
        }
    }

    private fun hentPersongalleri(behandlingId: UUID): Persongalleri = runBlocking {
        behandlingClient.hentPersongalleri(behandlingId)
    }

    private fun hentSoesken(persongalleri: Persongalleri): List<String> {
        return persongalleri.soesken
    }

    private fun hentForeldre(persongalleri: Persongalleri): List<String> {
        return persongalleri.avdoed
    }

    private fun vedtakTilStoenadsrad(vedtak: Vedtak): StoenadRad {
        val persongalleri = hentPersongalleri(behandlingId = vedtak.behandling.id)
        return StoenadRad(
            -1,
            vedtak.sak.ident,
            hentForeldre(persongalleri),
            hentSoesken(persongalleri),
            "40",
            vedtak.beregning?.sammendrag?.firstOrNull()?.beloep.toString(),
            "FOLKETRYGD",
            "",
            vedtak.behandling.id,
            vedtak.sak.id,
            vedtak.sak.id,
            Instant.now(),
            vedtak.sak.sakType,
            "",
            vedtak.vedtakFattet!!.ansvarligSaksbehandler,
            vedtak.attestasjon?.attestant,
            vedtak.virk.fom.atDay(1),
            vedtak.virk.tom?.atEndOfMonth()
        )
    }

    override fun slettSak(sakId: Long) {
        repository.slettSak(sakId)
    }
}