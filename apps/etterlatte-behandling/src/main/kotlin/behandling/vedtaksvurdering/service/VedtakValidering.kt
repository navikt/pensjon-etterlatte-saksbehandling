package no.nav.etterlatte.behandling.vedtaksvurdering.service

import no.nav.etterlatte.behandling.vedtaksvurdering.UgyldigAttestantException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

fun verifiserGyldigVedtakStatus(
    gjeldendeStatus: VedtakStatus,
    forventetStatus: List<VedtakStatus>,
) {
    if (gjeldendeStatus !in
        forventetStatus
    ) {
        throw InternfeilException("Vedtak har status $gjeldendeStatus, men forventet status $forventetStatus")
    }
}

fun sjekkAttestantHarAnnenIdentEnnSaksbehandler(
    ansvarligSaksbehandler: String,
    innloggetBrukerTokenInfo: BrukerTokenInfo,
) {
    if (innloggetBrukerTokenInfo.erSammePerson(ansvarligSaksbehandler)) {
        throw UgyldigAttestantException(innloggetBrukerTokenInfo.ident())
    }
}
