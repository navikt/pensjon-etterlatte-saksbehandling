package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo

fun opprettAvsenderRequest(
    bruker: BrukerTokenInfo,
    forenkletVedtak: ForenkletVedtak?,
    enhet: String,
) = forenkletVedtak?.let {
    AvsenderRequest(
        saksbehandlerIdent = it.saksbehandlerIdent,
        sakenhet = it.sakenhet,
        attestantIdent = it.attestantIdent,
    )
} ?: AvsenderRequest(saksbehandlerIdent = bruker.ident(), sakenhet = enhet)
