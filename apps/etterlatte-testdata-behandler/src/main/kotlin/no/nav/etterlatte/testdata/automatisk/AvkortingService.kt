package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagFlereInntekterDto
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
        virkningstidspunkt: YearMonth,
        bruker: BrukerTokenInfo,
    ) = retryOgPakkUt {
        klient
            .post(
                Resource(clientId, "$url/api/beregning/avkorting/$behandlingId/liste"),
                bruker,
                AvkortingGrunnlagFlereInntekterDto(
                    inntekter =
                        listOf(
                            AvkortingGrunnlagLagreDto(
                                inntektTom = 200_000,
                                fratrekkInnAar = 50_000,
                                inntektUtlandTom = 0,
                                fratrekkInnAarUtland = 0,
                                spesifikasjon = "kun test",
                                fom = virkningstidspunkt,
                            ),
                        ),
                ),
            ).mapBoth(
                success = {},
                failure = { throw it },
            )
    }
}
