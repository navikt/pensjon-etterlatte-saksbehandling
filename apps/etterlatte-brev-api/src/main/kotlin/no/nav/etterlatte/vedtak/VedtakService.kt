package no.nav.etterlatte.vedtak

import no.nav.etterlatte.libs.common.vedtak.Vedtak

interface VedtakService {
    fun hentVedtak(behandlingId: String): Vedtak
}