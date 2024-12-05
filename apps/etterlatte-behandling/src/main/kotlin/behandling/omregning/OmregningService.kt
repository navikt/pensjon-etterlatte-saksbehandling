package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.KjoeringDistEllerIverksattRequest
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import no.nav.etterlatte.oppgave.OppgaveService

class OmregningService(
    private val behandlingService: BehandlingService,
    private val omregningDao: OmregningDao,
    private val oppgaveService: OppgaveService,
) {
    fun hentSakerTilOmregning(
        kjoering: String,
        antall: Int,
        ekskluderteSaker: List<SakId>,
    ): List<Pair<SakId, KjoeringStatus>> = omregningDao.hentSakerTilOmregning(kjoering, antall, ekskluderteSaker)

    fun oppdaterKjoering(request: KjoeringRequest) = oppdaterKjoering(request, HardkodaSystembruker.omregning)

    fun oppdaterKjoering(
        request: KjoeringRequest,
        bruker: BrukerTokenInfo,
    ) {
        if (request.status == KjoeringStatus.FEILA) {
            omregningDao.hentNyligsteLinjeForKjoering(request.kjoering, request.sakId)?.let { (_, sisteStatus) ->
                if (sisteStatus.erFerdigstilt()) {
                    logger.error(
                        "Omregning har kjørt og feilet etter å ha blitt ferdigstilt. kjøring=${request.kjoering}, sak=${request.sakId}, feilendeSteg=${request.feilendeSteg}",
                    )
                    return
                }
            }

            behandlingService.hentAapenOmregning(request.sakId)?.let { omregning ->

                if (omregning.status.kanAvbrytes()) {
                    behandlingService.avbrytBehandling(omregning.id, bruker)
                }

                if (omregning.revurderingsaarsak == Revurderingaarsak.INNTEKTSENDRING) {
                    val oppgave =
                        oppgaveService
                            .hentOppgaverForReferanse(omregning.id.toString())
                            .singleOrNull { it.type === OppgaveType.INNTEKTSOPPLYSNING && it.erAttestering() }
                            ?: throw InternfeilException("Kan ikke eksistere en INNTEKTSENDRING uten oppgave")

                    if (oppgave.saksbehandler?.navn == Fagsaksystem.EY.navn) {
                        oppgaveService.fjernSaksbehandler(oppgave.id)
                        return
                    }
                }
            }
        }

        omregningDao.oppdaterKjoering(request)
    }

    fun kjoeringFullfoert(request: LagreKjoeringRequest) {
        if (!listOf(KjoeringStatus.FERDIGSTILT, KjoeringStatus.FERDIGSTILT_FATTET).contains(request.status)) {
            throw IllegalStateException("Prøver å lagre at kjøring er fullført, men status er ikke ferdigstilt.")
        }
        if (request.status == KjoeringStatus.FERDIGSTILT_FATTET) {
            request.behandling?.let {
                val oppgave = oppgaveService.hentOppgaverForReferanse(it.toString()).single()
                oppgaveService.fjernSaksbehandler(oppgave.id)
            }
        }
        omregningDao.lagreKjoering(request)
    }

    fun lagreDistribuertBrevEllerIverksattBehandlinga(request: KjoeringDistEllerIverksattRequest) =
        omregningDao.hentNyligsteLinjeForKjoering(request.kjoering, request.sakId)?.let { (_, sisteStatus) ->
            omregningDao.lagreDistribuertBrevEllerIverksattBehandlinga(request, sisteStatus)
        }
}
