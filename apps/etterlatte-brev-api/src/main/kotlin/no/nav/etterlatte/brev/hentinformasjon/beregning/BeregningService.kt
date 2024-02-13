package no.nav.etterlatte.brev.hentinformasjon.beregning

import no.nav.etterlatte.brev.hentinformasjon.BeregningKlient
import no.nav.etterlatte.token.BrukerTokenInfo

class BeregningService(private val beregningKlient: BeregningKlient) {
    suspend fun hentGrunnbeloep(bruker: BrukerTokenInfo) = beregningKlient.hentGrunnbeloep(bruker)
}
