package no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.retryOgPakkUt
import java.util.UUID

class AvkortingService(private val klient: HttpClient, private val url: String) {
    suspend fun avkort(behandlingId: UUID) =
        retryOgPakkUt {
            klient.post("$url/api/beregning/avkorting/$behandlingId") {
                contentType(ContentType.Application.Json)
                setBody(
                    AvkortingGrunnlagLagreDto(
                        aarsinntekt = 0,
                        fratrekkInnAar = 0,
                        inntektUtland = 0,
                        fratrekkInnAarUtland = 0,
                        spesifikasjon = "kun test",
                    ),
                )
            }
        }
}
