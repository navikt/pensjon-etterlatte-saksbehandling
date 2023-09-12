package no.nav.etterlatte.tilbakekreving.klienter

import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag
import org.slf4j.LoggerFactory

class BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        logger.info("Oppretter tilbakekreving i behandling")
        // TODO sett opp kall mot endepunkt når dette er på plass (EY-2662)
    }
}