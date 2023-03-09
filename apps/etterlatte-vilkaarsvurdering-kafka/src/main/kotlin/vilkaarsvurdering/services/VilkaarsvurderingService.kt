package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import java.util.*

interface VilkaarsvurderingService {
    fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID)
}

class VilkaarsvurderingServiceImpl(private val VilkaarsvurderingKlient: HttpClient, private val url: String) :
    VilkaarsvurderingService {
    override fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID) {
        runBlocking { VilkaarsvurderingKlient.post("$url/api/$behandlingId/kopier") }
    }
}