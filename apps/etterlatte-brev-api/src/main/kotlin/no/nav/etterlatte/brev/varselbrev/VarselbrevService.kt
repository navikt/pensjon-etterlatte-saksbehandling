package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.brev.Brevoppretter
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.Brevtype
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

internal class VarselbrevService(private val db: BrevRepository, private val brevoppretter: Brevoppretter) {
    fun hentVarselbrev(behandlingId: UUID) = db.hentBrevForBehandling(behandlingId, Brevtype.VARSEL)

    suspend fun opprettVarselbrev(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        brevoppretter.opprettVedtaksbrev(
            sakId = sakId,
            behandlingId = behandlingId,
            brukerTokenInfo = brukerTokenInfo,
        )
}
