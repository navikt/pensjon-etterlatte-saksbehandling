package no.nav.etterlatte.brev

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

class MigreringBrevDataService(private val brevdataFacade: BrevdataFacade) {
    suspend fun opprettMigreringBrevdata(
        generellBrevData: GenerellBrevData,
        migrering: MigreringBrevRequest?,
        brukerTokenInfo: BrukerTokenInfo,
    ): BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall {
        if (generellBrevData.systemkilde != Vedtaksloesning.PESYS) {
            throw InternfeilException("Kan ikke opprette et migreringsbrev fra pesys hvis kilde ikke er pesys")
        }
        return coroutineScope {
            val virkningstidspunkt =
                requireNotNull(generellBrevData.forenkletVedtak!!.virkningstidspunkt) {
                    "Migreringsvedtaket må ha et virkningstidspunkt"
                }

            val utbetalingsinfo =
                brevdataFacade.finnUtbetalingsinfo(
                    generellBrevData.behandlingId!!,
                    virkningstidspunkt,
                    brukerTokenInfo,
                    generellBrevData.sak.sakType,
                )
            BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall.fra(generellBrevData, utbetalingsinfo, migrering)
        }
    }
}
