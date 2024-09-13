package no.nav.etterlatte.testdata.automatisk

import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.YearMonth
import java.util.UUID

class AvkortingService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun avkort(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = retryOgPakkUt {
        klient.post(
            Resource(clientId, "$url/api/beregning/avkorting/$behandlingId"),
            bruker,
            AvkortingGrunnlagLagreDto(
                aarsinntekt = 200_000,
                fratrekkInnAar = 50_000,
                inntektUtland = 0,
                fratrekkInnAarUtland = 0,
                spesifikasjon = "kun test",
                fom = YearMonth.now(),
            ),
        )
    }
}
