package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingFactory
import no.nav.etterlatte.behandling.BehandlingHendelserKafkaProducer
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.GyldighetsproevingService
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.time.temporal.ChronoUnit
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

    suspend fun migrer(request: MigreringRequest) =
        retryMedPause(times = 3) {
            val sak =
                inTransaction {
                    finnEllerOpprettSak(request)
                }
            inTransaction {
                val behandlinger =
                    behandlingService.hentBehandlingerForSak(sak.id).filter { it.status != BehandlingStatus.AVBRUTT }
                if (behandlinger.any { it.status == BehandlingStatus.IVERKSATT } ||
                    behandlinger.any { it.kilde != Vedtaksloesning.GJENOPPRETTA }
                ) {
                    throw FinnesLoependeEllerIverksattBehandlingForFnr()
                }

                opprettSakOgBehandling(request, sak)?.let { behandlingOgOppgave ->
                    val behandling = behandlingOgOppgave.behandling
                    if (behandling.type != BehandlingType.FØRSTEGANGSBEHANDLING) {
                        throw IllegalArgumentException(
                            "Finnes allerede behandling for sak=${behandling.sak.id}. Stopper migrering for pesysId=${request.pesysId}",
                        )
                    }
                    val gjenoppretta = Vedtaksloesning.GJENOPPRETTA.name
                    kommerBarnetTilGodeService.lagreKommerBarnetTilgode(
                        KommerBarnetTilgode(
                            JaNei.JA,
                            "Automatisk gjenoppretta basert på opphørt sak fra Pesys",
                            Grunnlagsopplysning.Pesys.create(),
                            behandlingId = behandling.id,
                        ),
                    )
                    gyldighetsproevingService.lagreGyldighetsproeving(
                        behandling.id,
                        gjenoppretta,
                        JaNeiMedBegrunnelse(JaNei.JA, "Automatisk gjenoppretta basert på opphørt sak fra Pesys"),
                    )

                    val virkningstidspunktForMigrering = YearMonth.of(2024, 1)
                    behandlingService.oppdaterVirkningstidspunkt(
                        behandling.id,
                        virkningstidspunktForMigrering,
                        gjenoppretta,
                        "Automatisk gjenoppretta basert på opphørt sak fra Pesys",
                    )

                    sakService.oppdaterFlyktning(
                        sakId = behandling.sak.id,
                        flyktning =
                            Flyktning(
                                erFlyktning = request.flyktningStatus,
                                virkningstidspunkt = request.foersteVirkningstidspunkt.atDay(1),
                                begrunnelse = "Automatisk gjenoppretta basert på opphørt sak fra Pesys",
                                kilde = Grunnlagsopplysning.Pesys.create(),
                            ),
                    )

                    request.utlandstilknytningType?.let { utlandstilknytning ->
                        behandlingService.oppdaterUtlandstilknytning(
                            behandlingId = behandling.id,
                            utlandstilknytning =
                                Utlandstilknytning(
                                    type = utlandstilknytning,
                                    kilde = Grunnlagsopplysning.Pesys.create(),
                                    begrunnelse = "Automatisk gjenoppretta basert på opphørt sak fra Pesys",
                                ),
                        )
                    }

                    if (request.harMindreEnn40AarsTrygdetid() || request.erEoesBeregnet()) {
                        behandlingService.oppdaterBoddEllerArbeidetUtlandet(
                            behandlingId = behandling.id,
                            boddEllerArbeidetUtlandet =
                                BoddEllerArbeidetUtlandet(
                                    boddEllerArbeidetUtlandet = true,
                                    boddArbeidetEosNordiskKonvensjon = request.erEoesBeregnet().takeIf { it },
                                    kilde = Grunnlagsopplysning.Pesys.create(),
                                    begrunnelse =
                                        "Automatisk vurdert ved gjenoppretting fra Pesys. Vurdering av utlandsopphold kan være mangelfull.",
                                ),
                        )
                    }

                    val nyopprettaOppgave =
                        requireNotNull(behandlingOgOppgave.oppgave) {
                            "Mangler oppgave for behandling=${behandling.id}. Stopper gjenoppretting for pesysId=${request.pesysId}"
                        }
                    oppgaveService.tildelSaksbehandler(nyopprettaOppgave.id, gjenoppretta)

                    behandlingsHendelser.sendMeldingForHendelseMedDetaljertBehandling(
                        behandling.toStatistikkBehandling(request.opprettPersongalleri(), pesysId = request.pesysId.id),
                        BehandlingHendelseType.OPPRETTET,
                    )
                    behandling
                }
            }
        }

    fun opprettOppgaveManuellGjenoppretting(request: MigreringRequest) {
        val sak = finnEllerOpprettSak(request)
        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = request.pesysId.toString(),
            sakId = sak.id,
            oppgaveKilde = OppgaveKilde.BEHANDLING,
            oppgaveType = OppgaveType.FOERSTEGANGSBEHANDLING,
            merknad = "Oppgave for manuell gjenoppretting av opphørt sak i Pesys",
            frist = Tidspunkt.now().plus(5, ChronoUnit.DAYS),
        )
    }

    private suspend fun <T> retryMedPause(
        times: Int = 2,
        block: suspend () -> T,
    ) = retry(times, block).also { Thread.sleep(2000) }

    private fun opprettSakOgBehandling(
        request: MigreringRequest,
        sak: Sak,
    ) = behandlingFactory.opprettBehandling(
        sakId = sak.id,
        persongalleri = request.opprettPersongalleri(),
        mottattDato = null,
        kilde = Vedtaksloesning.GJENOPPRETTA,
        prosessType = Prosesstype.AUTOMATISK,
    )

    private fun finnEllerOpprettSak(request: MigreringRequest) =
        sakService.finnEllerOpprettSak(
            request.soeker.value,
            SakType.BARNEPENSJON,
            request.enhet.nr,
            sjekkEnhetMotNorg = false,
        )

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

class FinnesLoependeEllerIverksattBehandlingForFnr : Exception()
