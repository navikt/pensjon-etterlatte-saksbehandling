package no.nav.etterlatte.brev.hentinformasjon.trygdetid

import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class TrygdetidService(
    private val klient: TrygdetidKlient,
) {
    suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = klient.hentTrygdetid(behandlingId, brukerTokenInfo)
}
