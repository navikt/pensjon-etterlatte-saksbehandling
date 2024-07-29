package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.readValue
import java.util.UUID

class GrunnlagService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun hentGrunnlagForBehandling(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Grunnlag =
        retryOgPakkUt {
            klient.get(Resource(clientId, "$url/api/grunnlag/behandling/$behandlingId"), bruker).mapBoth(
                success = { readValue(it) },
                failure = { throw it },
            )
        }
}
