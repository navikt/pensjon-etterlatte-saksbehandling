package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold

class BrevService(private val db: BrevRepository) {
    fun hentBrev(id: BrevID): Brev = db.hentBrev(id)

    fun hentBrevInnhold(id: BrevID): BrevInnhold = db.hentBrevInnhold(id)
}