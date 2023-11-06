package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingHendelseType
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GyldighetsproevingService
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
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class MigreringService(
    private val sakService: SakService,
    private val gyldighetsproevingService: GyldighetsproevingService,
    private val behandlingFactory: BehandlingFactory,
    private val kommerBarnetTilGodeService: KommerBarnetTilGodeService,
    private val behandlingsHendelser: BehandlingHendelserKafkaProducer,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun migrer(request: MigreringRequest) =
        inTransaction {
            opprettSakOgBehandling(request)?.let { behandlingOgOppgave ->
                val behandling = behandlingOgOppgave.behandling
                val pesys = Vedtaksloesning.PESYS.name
                kommerBarnetTilGodeService.lagreKommerBarnetTilgode(
                    KommerBarnetTilgode(
                        JaNei.JA,
                        "Automatisk importert fra Pesys",
                        Grunnlagsopplysning.Pesys.create(),
                        behandlingId = behandling.id,
                    ),
                )
                gyldighetsproevingService.lagreGyldighetsproeving(
                    behandling.id,
                    pesys,
                    JaNeiMedBegrunnelse(JaNei.JA, "Automatisk importert fra Pesys"),
                )

                val virkningstidspunktForMigrering = YearMonth.of(2024, 1)
                behandlingService.oppdaterVirkningstidspunkt(
                    behandling.id,
                    virkningstidspunktForMigrering,
                    pesys,
                    "Automatisk importert fra Pesys",
                )
                val nyopprettaOppgave = requireNotNull(behandlingOgOppgave.oppgave)
                oppgaveService.tildelSaksbehandler(nyopprettaOppgave.id, pesys)

                behandlingsHendelser.sendMeldingForHendelseMedDetaljertBehandling(
                    behandling.toStatistikkBehandling(request.opprettPersongalleri()),
                    BehandlingHendelseType.OPPRETTET,
                )
                behandling
            }
        }

    private fun opprettSakOgBehandling(request: MigreringRequest) =
        behandlingFactory.opprettBehandling(
            finnEllerOpprettSak(request).id,
            request.opprettPersongalleri(),
            null,
            Vedtaksloesning.PESYS,
        )

    private fun finnEllerOpprettSak(request: MigreringRequest) =
        sakService.finnEllerOpprettSak(request.soeker.value, SakType.BARNEPENSJON, request.enhet.nr)

    fun avbrytBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val status = behandlingService.hentBehandling(behandlingId)!!.status
        if (!status.kanAvbrytes()) {
            logger.warn("Behandling $behandlingId kan ikke avbrytes, fordi den har status $status.")
            return
        }
        behandlingService.avbrytBehandling(behandlingId, brukerTokenInfo)
    }
}
