package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.foerstegangsbehandling.FoerstegangsbehandlingService
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.behandling.migrering.MigreringRepository
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.sak.SakService

class MigreringService(
    private val sakService: SakService,
    private val foerstegangsBehandlingService: FoerstegangsbehandlingService,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    private val behandlingsHendelser: BehandlingHendelserKafkaProducer,
    private val migreringRepository: MigreringRepository,
    private val behandlingService: BehandlingService
) {
    fun migrer(request: MigreringRequest) = opprettSakOgBehandling(request)?.let {
        val pesys = Vedtaksloesning.PESYS.name
        kommerBarnetTilGodeService.lagreKommerBarnetTilgode(
            KommerBarnetTilgode(
                JaNei.JA,
                "Automatisk importert fra Pesys",
                Grunnlagsopplysning.Pesys.create(),
                behandlingId = it.id
            )
        )
        foerstegangsBehandlingService.lagreGyldighetsproeving(
            it.id,
            pesys,
            JaNeiMedBegrunnelse(JaNei.JA, "Automatisk importert fra Pesys")
        )
        behandlingService.oppdaterVirkningstidspunkt(
            it.id,
            request.virkningstidspunkt,
            pesys,
            "Automatisk importert fra Pesys"
        )
        behandlingsHendelser.sendMeldingForHendelse(it, BehandlingHendelseType.OPPRETTET)
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
}