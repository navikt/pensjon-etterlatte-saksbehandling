package no.nav.etterlatte.vedtak

import no.nav.etterlatte.domene.vedtak.Vedtak

interface VedtakService {
    fun hentVedtak(vedtakId: Long): Vedtak
}
