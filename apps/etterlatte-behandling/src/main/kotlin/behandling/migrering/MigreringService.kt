package no.nav.etterlatte.behandling.omregning

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKanal
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.migrering.MigreringRepository
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.sak.SakService
import java.util.UUID

class MigreringService(
    private val sakService: SakService,
    private val foerstegangsBehandlingService: FoerstegangsbehandlingService,
    private val behandlingsHendelser: BehandlingHendelserKanal,
    private val migreringRepository: MigreringRepository,
    private val generellBehandlingService: GenerellBehandlingService
) {
    fun migrer(request: MigreringRequest) = opprettSakOgBehandling(request)?.let {
        val pesys = Vedtaksloesning.PESYS.name
        foerstegangsBehandlingService.lagreKommerBarnetTilgode(
            it.id,
            KommerBarnetTilgode(
                JaNei.JA,
                "Automatisk importert fra Pesys",
                Grunnlagsopplysning.Pesys.create()
            )
        )
        foerstegangsBehandlingService.lagreGyldighetsproeving(
            it.id,
            pesys,
            JaNeiMedBegrunnelse(JaNei.JA, "Automatisk importert fra Pesys")
        )
        generellBehandlingService.oppdaterVirkningstidspunkt(
            it.id,
            request.virkningstidspunkt,
            pesys,
            "Automatisk importert fra Pesys"
        )
        runBlocking { sendHendelse(it.id, it.type) }
        migreringRepository.lagreKoplingTilPesyssaka(pesysSakId = request.pesysId, sakId = it.sak.id)
        it
    }

    private fun opprettSakOgBehandling(request: MigreringRequest): Behandling? =
        foerstegangsBehandlingService.opprettBehandling(
            finnEllerOpprettSak(request).id,
            request.persongalleri,
            null,
            Vedtaksloesning.PESYS
        )

    private fun finnEllerOpprettSak(request: MigreringRequest) =
        sakService.finnEllerOpprettSak(request.fnr.value, SakType.BARNEPENSJON, request.enhet.nr)

    private suspend fun sendHendelse(behandlingId: UUID, type: BehandlingType) =
        behandlingsHendelser.send(Triple(behandlingId, BehandlingHendelseType.OPPRETTET, type))
}