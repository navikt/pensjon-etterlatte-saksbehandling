package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GyldighetsproevingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.sak.SakService

class MigreringService(
    private val sakService: SakService,
    private val gyldighetsproevingService: GyldighetsproevingService,
    private val behandlingFactory: BehandlingFactory,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    private val behandlingsHendelser: BehandlingHendelserKafkaProducer,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {
    fun migrer(request: MigreringRequest) =
        opprettSakOgBehandling(request)?.let {
            val pesys = Vedtaksloesning.PESYS.name
            kommerBarnetTilGodeService.lagreKommerBarnetTilgode(
                KommerBarnetTilgode(
                    JaNei.JA,
                    "Automatisk importert fra Pesys",
                    Grunnlagsopplysning.Pesys.create(),
                    behandlingId = it.id,
                ),
            )
            gyldighetsproevingService.lagreGyldighetsproeving(
                it.id,
                pesys,
                JaNeiMedBegrunnelse(JaNei.JA, "Automatisk importert fra Pesys"),
            )
            inTransaction {
                behandlingService.oppdaterVirkningstidspunkt(
                    it.id,
                    request.virkningstidspunkt,
                    pesys,
                    "Automatisk importert fra Pesys",
                )
            }
            val nyopprettaOppgave =
                oppgaveService.hentOppgaverForSak(it.sak.id).first { o -> o.referanse == it.id.toString() }
            oppgaveService.tildelSaksbehandler(nyopprettaOppgave.id, pesys)

            behandlingsHendelser.sendMeldingForHendelseMedDetaljertBehandling(
                it.toStatistikkBehandling(request.opprettPersongalleri()),
                BehandlingHendelseType.OPPRETTET,
            )
            it
        }

    private fun opprettSakOgBehandling(request: MigreringRequest): Behandling? =
        behandlingFactory.opprettBehandling(
            finnEllerOpprettSak(request).id,
            request.opprettPersongalleri(),
            null,
            Vedtaksloesning.PESYS,
        )

    private fun finnEllerOpprettSak(request: MigreringRequest) =
        sakService.finnEllerOpprettSak(request.soeker.value, SakType.BARNEPENSJON, request.enhet.nr)
}
