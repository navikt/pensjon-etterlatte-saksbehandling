package no.nav.etterlatte.libs.common.brev.model

import no.nav.etterlatte.libs.common.journalpost.AvsenderMottaker
import no.nav.etterlatte.libs.common.journalpost.Bruker

data class DistribusjonMelding(
    val vedtakId: String,
    val brevId: Long,
    val mottaker: AvsenderMottaker,
    val bruker: Bruker,
    val tittel: String
)
