package no.nav.etterlatte.vedtaksvurdering.klienter

import no.nav.etterlatte.vedtaksvurdering.Vedtak

interface SamKlient {
    /**
     * @return true=vente på samordning med tjenestepensjon, false=ikke vente
     */
    suspend fun samordneVedtak(vedtak: Vedtak): Boolean
}

class SamKlientImpl : SamKlient {
    override suspend fun samordneVedtak(vedtak: Vedtak): Boolean {
        return false
    }
}
