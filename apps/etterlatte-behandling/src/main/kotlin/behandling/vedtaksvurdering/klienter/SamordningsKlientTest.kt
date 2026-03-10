package no.nav.etterlatte.behandling.vedtaksvurdering.klienter

import no.nav.etterlatte.behandling.vedtaksvurdering.EtterbetalingResultat
import no.nav.etterlatte.behandling.vedtaksvurdering.OppdaterSamordningsmelding
import no.nav.etterlatte.behandling.vedtaksvurdering.Samordningsvedtak
import no.nav.etterlatte.behandling.vedtaksvurdering.Vedtak
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

class SamordningsKlientTest : SamordningsKlient {
    /**
     * @return true=vente på samordning med tjenestepensjon, false=ikke vente
     */
    override suspend fun samordneVedtak(
        vedtak: Vedtak,
        etterbetaling: EtterbetalingResultat,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * @param vedtak IDer som sendes til SAM
     */
    override suspend fun hentSamordningsdata(
        vedtak: Vedtak,
        alleVedtak: Boolean,
    ): List<Samordningsvedtak> {
        TODO("Not yet implemented")
    }

    override suspend fun oppdaterSamordningsmelding(
        samordningmelding: OppdaterSamordningsmelding,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }
}
