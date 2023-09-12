package no.nav.etterlatte.tilbakekreving.kravgrunnlag

import no.nav.etterlatte.tilbakekreving.klienter.BehandlingKlient
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.slf4j.LoggerFactory

class KravgrunnlagService(private val behandlingKlient: BehandlingKlient) {

    fun opprettTilbakekreving(detaljertKravgrunnlagDto: DetaljertKravgrunnlagDto) {
        val kravgrunnlag = KravgrunnlagMapper.toKravgrunnlag(detaljertKravgrunnlagDto)
        behandlingKlient.opprettTilbakekreving(kravgrunnlag)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KravgrunnlagService::class.java)
    }
}