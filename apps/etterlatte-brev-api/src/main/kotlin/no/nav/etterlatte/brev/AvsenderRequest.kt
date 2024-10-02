package no.nav.etterlatte.brev

import no.nav.etterlatte.libs.common.Enhetsnummer

data class AvsenderRequest(
    val saksbehandlerIdent: String,
    val sakenhet: Enhetsnummer,
    val attestantIdent: String? = null,
)
