package no.nav.etterlatte.behandling.omregning

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker

class OmregningService(
    private val behandlingService: BehandlingService,
    private val omregningDao: OmregningDao,
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
            behandlingService.hentAapenOmregning(request.sakId)?.let {
                if (it.status.kanAvbrytes()) {
                    behandlingService.avbrytBehandling(it.id, bruker)
                }
            }
        }
        omregningDao.oppdaterKjoering(request)
    }

    fun kjoeringFullfoert(request: LagreKjoeringRequest) {
        if (request.status != KjoeringStatus.FERDIGSTILT) {
            throw IllegalStateException("Prøver å lagre at kjøring er fullført, men status er ikke ferdigstilt.")
        }
        omregningDao.lagreKjoering(request)
    }
}
