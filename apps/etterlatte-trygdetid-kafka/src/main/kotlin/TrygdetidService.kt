package no.nav.etterlatte.trygdetid.kafka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import java.util.*

class TrygdetidService(
    private val trygdetidApp: HttpClient,
    private val url: String
) {

    fun beregnTrygdetid(behandlingId: UUID): TrygdetidDto = runBlocking {
        trygdetidApp.post("$url/api/trygdetid/$behandlingId") {
            contentType(ContentType.Application.Json)
        }.body()
    }

    fun beregnTrygdetidGrunnlag(behandlingId: UUID, grunnlag: TrygdetidGrunnlagDto): TrygdetidDto = runBlocking {
        trygdetidApp.post("$url/api/trygdetid/$behandlingId/grunnlag") {
            contentType(ContentType.Application.Json)
            setBody(grunnlag)
        }.body()
    }

    fun regulerTrygdetid(behandlingId: UUID, forrigeBehandlingId: UUID) = runBlocking {
        trygdetidApp.post("$url/api/trygdetid/$behandlingId/reguler/$forrigeBehandlingId") {
            contentType(ContentType.Application.Json)
        }
    }
}