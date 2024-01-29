package no.nav.etterlatte.brev

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.hentinformasjon.BrevdataFacade
import no.nav.etterlatte.brev.model.bp.OmregnetBPNyttRegelverk
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.token.BrukerTokenInfo

class MigreringBrevDataService(private val brevdataFacade: BrevdataFacade) {
    suspend fun opprettMigreringBrevdata(
        generellBrevData: GenerellBrevData,
        migrering: MigreringBrevRequest?,
        brukerTokenInfo: BrukerTokenInfo,
    ): OmregnetBPNyttRegelverk {
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
            OmregnetBPNyttRegelverk.fra(generellBrevData, utbetalingsinfo, migrering)
        }
    }
}
