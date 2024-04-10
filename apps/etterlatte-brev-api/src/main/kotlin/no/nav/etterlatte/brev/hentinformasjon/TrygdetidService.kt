package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class TrygdetidService(private val trygdetidKlient: TrygdetidKlient) {
    suspend fun finnTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo)
}
