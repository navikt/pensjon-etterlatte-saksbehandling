package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagFlereInntekterDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class AvkortingService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    private val aarsinntekt: Int = 200_000

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
                        listOfNotNull(
                            AvkortingGrunnlagLagreDto(
                                inntektTom = aarsinntekt,
                                fratrekkInnAar = fratrekkInnAarFramTil(virkningstidspunkt),
                                inntektUtlandTom = 0,
                                fratrekkInnAarUtland = 0,
                                spesifikasjon = "kun test",
                                fom = virkningstidspunkt,
                            ),
                            if (LocalDate.now().month >= Month.OCTOBER &&
                                virkningstidspunkt.year == YearMonth.now().year
                            ) {
                                AvkortingGrunnlagLagreDto(
                                    inntektTom = aarsinntekt,
                                    fratrekkInnAar = 0,
                                    inntektUtlandTom = 0,
                                    fratrekkInnAarUtland = 0,
                                    spesifikasjon = "kun test",
                                    fom = YearMonth.of(virkningstidspunkt.year + 1, 1),
                                )
                            } else {
                                null
                            },
                        ),
                ),
            ).mapBoth(
                success = {},
                failure = { throw it },
            )
    }

    private fun fratrekkInnAarFramTil(virkningstidspunkt: YearMonth): Int = aarsinntekt * (virkningstidspunkt.month.value - 1) / 12
}
