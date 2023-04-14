package no.nav.etterlatte.behandling.omregning

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.migrering.MigreringRepository
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.sak.SakService
import rapidsandrivers.migrering.MigreringRequest
import java.time.format.DateTimeFormatter
import java.util.*

class MigreringService(
    private val sakService: SakService,
    private val foerstegangsbehandlingService: FoerstegangsbehandlingService,
    private val behandlingsHendelser: BehandlingHendelserKanal,
    private val migreringRepository: MigreringRepository
) {
    fun migrer(request: MigreringRequest): Behandling {
        val behandling = opprettSakOgBehandling(request)
        runBlocking { sendHendelse(behandling.id) }
        migreringRepository.lagreKoplingTilPesyssaka(pesysSakId = request.pesysId, sakId = behandling.sak.id)
        return behandling
    }

    private fun opprettSakOgBehandling(request: MigreringRequest): Behandling =
        foerstegangsbehandlingService.startFoerstegangsbehandling(
            finnEllerOpprettSak(request).id,
            request.persongalleri,
            request.mottattDato.format(DateTimeFormatter.ISO_DATE_TIME),
            Vedtaksloesning.PESYS
        )

    private fun finnEllerOpprettSak(request: MigreringRequest) =
        inTransaction { sakService.finnEllerOpprettSak(request.fnr.value, SakType.BARNEPENSJON, request.enhet.nr) }

    private suspend fun sendHendelse(behandlingId: UUID) =
        behandlingsHendelser.send(behandlingId to BehandlingHendelseType.OPPRETTET)
}