package no.nav.etterlatte.libs.journalpost.dokarkiv

import no.nav.etterlatte.libs.journalpost.felles.Bruker
import no.nav.etterlatte.libs.journalpost.felles.Sakstype

data class KnyttTilAnnenSakRequest(
    val bruker: Bruker,
    val fagsakId: String,
    val fagsaksystem: String,
    val journalfoerendeEnhet: String,
    val tema: String,
    val sakstype: Sakstype,
)

data class KnyttTilAnnenSakResponse(
    val nyJournalpostId: String,
)
