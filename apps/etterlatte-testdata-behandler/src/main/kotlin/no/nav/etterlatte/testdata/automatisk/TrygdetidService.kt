package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.util.UUID

class TrygdetidService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun beregnTrygdetid(behandlingId: UUID) =
        retryOgPakkUt {
            klient.post(Resource(clientId, "$url/api/trygdetid/$behandlingId"), Systembruker.testdata) {}
        }.mapBoth(
            success = {},
            failure = { throw it },
        )
}
