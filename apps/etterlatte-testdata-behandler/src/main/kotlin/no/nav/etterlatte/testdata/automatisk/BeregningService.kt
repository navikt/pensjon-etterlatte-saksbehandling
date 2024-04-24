package no.nav.etterlatte.testdata.automatisk

import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.util.UUID

class BeregningService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun beregn(behandlingId: UUID) = klient.post(Resource(clientId, "$url/api/beregning/$behandlingId"), Systembruker.testdata) {}
}
