package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brevtype
import java.util.UUID

internal class VarselbrevService(private val db: BrevRepository) {
    fun hentVarselbrev(behandlingId: UUID) = db.hentBrevForBehandling(behandlingId, Brevtype.VARSEL)
}
