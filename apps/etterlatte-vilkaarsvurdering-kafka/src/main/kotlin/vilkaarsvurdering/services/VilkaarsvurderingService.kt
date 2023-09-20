package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import java.util.UUID

interface VilkaarsvurderingService {
    fun kopierForrigeVilkaarsvurdering(
        behandlingId: UUID,
        behandlingViOmregnerFra: UUID,
    ): HttpResponse

    fun opprettVilkaarsvurdering(behandlingId: UUID): HttpResponse

    fun migrer(behandlingId: UUID): HttpResponse
}

class VilkaarsvurderingServiceImpl(private val vilkaarsvurderingKlient: HttpClient, private val url: String) :
    VilkaarsvurderingService {
    override fun kopierForrigeVilkaarsvurdering(
        behandlingId: UUID,
        behandlingViOmregnerFra: UUID,
    ) = runBlocking {
        vilkaarsvurderingKlient.post("$url/api/vilkaarsvurdering/$behandlingId/kopier") {
            contentType(ContentType.Application.Json)
            setBody(OpprettVilkaarsvurderingFraBehandling(behandlingViOmregnerFra))
        }
    }

    override fun opprettVilkaarsvurdering(behandlingId: UUID) =
        runBlocking {
            vilkaarsvurderingKlient.post("$url/api/vilkaarsvurdering/$behandlingId/opprett") {
                contentType(ContentType.Application.Json)
            }
        }

    override fun migrer(behandlingId: UUID) =
        runBlocking {
            vilkaarsvurderingKlient.post(
                "$url/api/vilkaarsvurdering/migrering/$behandlingId",
            )
        }
}
