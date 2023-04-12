package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import vilkaarsvurdering.VilkaarsvurderingAPI
import java.util.*

interface VilkaarsvurderingService {
    fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID)
    fun opprettVilkaarsvurdering(behandlingId: UUID): HttpResponse
}

class VilkaarsvurderingServiceImpl(private val vilkaarsvurderingKlient: HttpClient, private val url: String) :
    VilkaarsvurderingService {
    override fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID) {
        runBlocking {
            vilkaarsvurderingKlient.post(
                "url/${VilkaarsvurderingAPI.basisrute}/${VilkaarsvurderingAPI.kopier(behandlingId.toString())}"
            ) {
                contentType(ContentType.Application.Json)
                setBody(OpprettVilkaarsvurderingFraBehandling(behandlingViOmregnerFra))
            }
        }
    }

    override fun opprettVilkaarsvurdering(behandlingId: UUID) = runBlocking {
        vilkaarsvurderingKlient.post(
            "url/${VilkaarsvurderingAPI.basisrute}/${VilkaarsvurderingAPI.opprett(behandlingId.toString())}"
        ) {
            contentType(ContentType.Application.Json)
        }
    }
}