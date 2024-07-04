package no.nav.etterlatte.brev

import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.hentinformasjon.beregning.BeregningService
import no.nav.etterlatte.brev.model.bp.BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.YearMonth
import java.util.UUID

class MigreringBrevDataService(
    private val beregningService: BeregningService,
) {
    suspend fun opprettMigreringBrevdata(
        brukerTokenInfo: BrukerTokenInfo,
        systemkilde: Vedtaksloesning,
        muligVirkningstidspunkt: YearMonth?,
        behandlingId: UUID,
        sakType: SakType,
        personerISak: PersonerISak,
        loependeIPesys: Boolean,
        utlandstilknytning: Utlandstilknytning?,
        erSystembruker: Boolean,
    ): BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall {
        if (systemkilde != Vedtaksloesning.PESYS) {
            throw InternfeilException("Kan ikke opprette et migreringsbrev fra pesys hvis kilde ikke er pesys")
        }
        return coroutineScope {
            val virkningstidspunkt =
                requireNotNull(muligVirkningstidspunkt) {
                    "Migreringsvedtaket må ha et virkningstidspunkt"
                }

            val utbetalingsinfo =
                beregningService.finnUtbetalingsinfo(
                    behandlingId,
                    virkningstidspunkt,
                    brukerTokenInfo,
                    sakType,
                )
            BarnepensjonOmregnetNyttRegelverkRedigerbartUtfall.fra(
                utbetalingsinfo,
                personerISak,
                loependeIPesys,
                utlandstilknytning,
                behandlingId,
                erSystembruker,
            )
        }
    }
}
