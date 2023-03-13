package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import java.util.*

interface VilkaarsvurderingService {
    fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID)
}

class VilkaarsvurderingServiceImpl(private val vilkaarsvurderingKlient: HttpClient, private val url: String) :
    VilkaarsvurderingService {
    override fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID) {
        runBlocking {
            vilkaarsvurderingKlient.post("$url/api/$behandlingId/kopier") {
                contentType(ContentType.Application.Json)
                setBody(OpprettVilkaarsvurderingFraBehandling(behandlingViOmregnerFra))
            }
        }
    }
}
data class OpprettVilkaarsvurderingFraBehandling(val forrigeBehandling: UUID)