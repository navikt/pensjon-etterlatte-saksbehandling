package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.tilbakekreving.klienter.BehandlingKlient
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto

class KravgrunnlagService(private val behandlingKlient: BehandlingKlient) {
    fun opprettTilbakekreving(detaljertKravgrunnlagDto: DetaljertKravgrunnlagDto) {
        val kravgrunnlag = KravgrunnlagMapper.toKravgrunnlag(detaljertKravgrunnlagDto)
        runBlocking {
            behandlingKlient.opprettTilbakekreving(kravgrunnlag)
        }
    }
}
