package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import org.slf4j.LoggerFactory
import java.util.UUID

class AutomatiskBehandlingService(
    val service: VedtakBehandlingService,
    val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun vedtakStegvis(
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
        kjoringVariant: MigreringKjoringVariant,
    ): VedtakOgRapid =
        when (kjoringVariant) {
            MigreringKjoringVariant.FULL_KJORING -> {
                val rapid1 = opprettOgFattVedtak(behandlingId, sakId, brukerTokenInfo)
                val rapid2 = attesterVedtak(behandlingId, brukerTokenInfo)
                VedtakOgRapid(
                    vedtak = rapid2.vedtak,
                    rapidInfo1 = rapid1.rapidInfo1,
                    rapidInfo2 = rapid2.rapidInfo1,
                )
            }

            MigreringKjoringVariant.MED_PAUSE -> opprettOgFattVedtak(behandlingId, sakId, brukerTokenInfo)
            MigreringKjoringVariant.FORTSETT_ETTER_PAUSE -> attesterVedtak(behandlingId, brukerTokenInfo)
        }

    private suspend fun opprettOgFattVedtak(
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Håndterer behandling $behandlingId")
        val vedtak = service.hentVedtakForBehandling(behandlingId, brukerTokenInfo)
        if (vedtak != null) {
            logger.warn(
                "Skal opprette og fatte vedtak, men har allerede et vedtak for behandlingen" +
                    " med id=$behandlingId med status ${vedtak.status}",
            )
            if (vedtak.status !in listOf(VedtakStatus.OPPRETTET, VedtakStatus.FATTET_VEDTAK)) {
                throw InternfeilException(
                    "Vi skal opprette og fatte vedtak, men vedtaket har allerde kommet " +
                        "lengre i statusflyten (${vedtak.status}). Da kan vi ikke trygt recovere fra situasjonen." +
                        " Noe må manuelt legges til for at denne meldingen skal passere ok.",
                )
            }
        }

        if (vedtak == null) {
            service.opprettEllerOppdaterVedtak(behandlingId, brukerTokenInfo)
        }
        val vedtakOgRapid =
            if (vedtak?.status != VedtakStatus.FATTET_VEDTAK) {
                logger.info("Fatter vedtak for behandling $behandlingId")
                service.fattVedtak(behandlingId, brukerTokenInfo, saksbehandler = Fagsaksystem.EY.navn)
            } else {
                logger.warn(
                    "Skal opprette og fatte vedtak, men har allerede et fattet vedtak for behandlingen" +
                        " med id=$behandlingId. Bare returnerer det.",
                )
                VedtakOgRapid(
                    vedtak.toDto(),
                    RapidInfo(
                        vedtakhendelse = VedtakKafkaHendelseHendelseType.FATTET,
                        vedtak = vedtak.toDto(),
                        tekniskTid = vedtak.vedtakFattet!!.tidspunkt,
                        behandlingId = behandlingId,
                    ),
                )
            }
        logger.info("Tildeler attesteringsoppgave til systembruker")
        val oppgaveTilAttestering =
            behandlingKlient
                .hentOppgaverForSak(sakId, brukerTokenInfo)
                .filter { it.referanse == behandlingId.toString() }
                .filter { it.status == Status.ATTESTERING }
                .first()
        behandlingKlient.tildelSaksbehandler(oppgaveTilAttestering, brukerTokenInfo)
        return vedtakOgRapid
    }

    private suspend fun attesterVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakOgRapid {
        logger.info("Attesterer vedtak for behandling $behandlingId")
        return service.attesterVedtak(
            behandlingId,
            "Automatisk attestert av ${Fagsaksystem.EY.systemnavn}",
            brukerTokenInfo,
            attestant = Fagsaksystem.EY.navn,
        )
    }
}
