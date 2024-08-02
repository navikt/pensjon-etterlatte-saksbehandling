package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.vilkaarsvurdering.MigrertYrkesskadefordel
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import java.util.UUID

interface VilkaarsvurderingService {
    fun kopierForrigeVilkaarsvurdering(
        behandlingId: UUID,
        behandlingViOmregnerFra: UUID,
    ): HttpResponse

    fun opprettVilkaarsvurdering(behandlingId: UUID): HttpResponse

    fun opphoerAldersovergang(
        behandlingId: UUID,
        behandlingKopiereFraId: UUID,
    ): HttpResponse

    fun harMigrertYrkesskadefordel(behandlingId: String): Boolean

    fun harRettUtenTidsbegrensning(behandlingId: String): Boolean
}

class VilkaarsvurderingServiceImpl(
    private val vilkaarsvurderingKlient: HttpClient,
    private val url: String,
) : VilkaarsvurderingService {
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

    override fun opphoerAldersovergang(
        behandlingId: UUID,
        behandlingKopiereFraId: UUID,
    ) = runBlocking {
        vilkaarsvurderingKlient.post("$url/api/vilkaarsvurdering/aldersovergang/$behandlingId?fra=$behandlingKopiereFraId") {
            contentType(ContentType.Application.Json)
        }
    }

    override fun harMigrertYrkesskadefordel(behandlingId: String): Boolean =
        runBlocking {
            vilkaarsvurderingKlient
                .get("$url/api/vilkaarsvurdering/$behandlingId/migrert-yrkesskadefordel")
                .body<MigrertYrkesskadefordel>()
                .migrertYrkesskadefordel
        }

    override fun harRettUtenTidsbegrensning(behandlingId: String): Boolean =
        runBlocking {
            vilkaarsvurderingKlient
                .get("$url/api/vilkaarsvurdering/$behandlingId/rett-uten-tidsbegrensning")
                .body<Map<String, Any>>()["rettUtenTidsbegrensning"] as Boolean
        }
}
