package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking

interface VilkaarsvurderingService {
    fun kopierForrigeVilkaarsvurdering(sakId: Long): Int
}

class VilkaarsvurderingServiceImpl(private val VilkaarsvurderingKlient: HttpClient, private val url: String) :
    VilkaarsvurderingService {
    override fun kopierForrigeVilkaarsvurdering(sakId: Long): Int {
        return runBlocking { VilkaarsvurderingKlient.post("$url/api/x").body() }
    }
}