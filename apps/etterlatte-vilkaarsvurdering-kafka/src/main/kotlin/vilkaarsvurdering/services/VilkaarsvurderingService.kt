package no.nav.etterlatte.vilkaarsvurdering.services

import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.ktor.post
import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling
import vilkaarsvurdering.VilkaarsvurderingAPI
import java.util.*

interface VilkaarsvurderingService {
    fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID): HttpResponse
    fun opprettVilkaarsvurdering(behandlingId: UUID): HttpResponse
}

class VilkaarsvurderingServiceImpl(private val vilkaarsvurderingKlient: HttpClient, private val url: String) :
    VilkaarsvurderingService {
    override fun kopierForrigeVilkaarsvurdering(behandlingId: UUID, behandlingViOmregnerFra: UUID) =
        runBlocking {
            vilkaarsvurderingKlient.post(
                VilkaarsvurderingAPI.kopier(behandlingId.toString())
                    .medBody(url) { OpprettVilkaarsvurderingFraBehandling(behandlingViOmregnerFra) }
            )
        }

    override fun opprettVilkaarsvurdering(behandlingId: UUID) = runBlocking {
        vilkaarsvurderingKlient.post(VilkaarsvurderingAPI.opprett(behandlingId.toString()).medBody(url) { })
    }
}