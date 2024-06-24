package no.nav.etterlatte.behandling.revurdering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class RevurderingKopierGrunnlag(
    private val featureToggleService: FeatureToggleService,
    private val vilkaarsvurderingKlient: VilkaarsvurderingKlient,
    private val trygdetidKlient: TrygdetidKlient,
    private val beregningKlient: BeregningKlient,
) {
    fun kopier(
        nyBehandling: UUID,
        forrigeBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (!featureToggleService.isEnabled(RevurderingFeatureToggle.KopierGrunnlag, false)) {
            return
        }
        runBlocking {
            vilkaarsvurderingKlient.kopierVilkaarsvurderingFraForrigeBehandling(nyBehandling, forrigeBehandling, brukerTokenInfo)
            trygdetidKlient.kopierTrygdetidFraForrigeBehandling(nyBehandling, forrigeBehandling, brukerTokenInfo)

            beregningKlient.kopierBeregningsgrunnlagFraForrigeBehandling(nyBehandling, forrigeBehandling, brukerTokenInfo)

            // TODO kopier overstyrt beregning?

            beregningKlient.opprettBeregning(nyBehandling, brukerTokenInfo)

            // TODO kun hvis oms
            beregningKlient.opprettAvkorting(nyBehandling, forrigeBehandling, brukerTokenInfo)
        }
    }
}
