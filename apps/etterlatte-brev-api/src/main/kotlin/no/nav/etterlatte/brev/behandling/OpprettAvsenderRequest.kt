package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.ktor.token.VanligSystembruker

fun opprettAvsenderRequest(
    bruker: BrukerTokenInfo,
    forenkletVedtak: ForenkletVedtak?,
    enhet: Enhet,
) = forenkletVedtak?.let {
    AvsenderRequest(
        saksbehandlerIdent = it.saksbehandlerIdent,
        sakenhet = it.sakenhet,
        attestantIdent = it.attestantIdent,
    )
} ?: AvsenderRequest(saksbehandlerIdent = hentBrevIdent(bruker), sakenhet = enhet)

private fun hentBrevIdent(bruker: BrukerTokenInfo): String =
    when (bruker) {
        is Saksbehandler -> bruker.ident
        is VanligSystembruker -> bruker.ident
        is HardkodaSystembruker -> Fagsaksystem.EY.navn
    }
