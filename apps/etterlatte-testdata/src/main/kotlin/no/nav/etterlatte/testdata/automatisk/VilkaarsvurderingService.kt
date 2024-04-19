package no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import org.slf4j.LoggerFactory
import java.util.UUID

class VilkaarsvurderingService(private val klient: HttpClient, private val url: String) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun vilkaarsvurder(behandlingId: UUID) {
        logger.info("Oppretter vilkårsvurdering for gjenoppretting for $behandlingId")
        val vilkaarsvurdering = retryOgPakkUt { opprettVilkaarsvurdering(behandlingId) }

        logger.info("Oppdaterer vilkårene med korrekt utfall for gjenoppretting $behandlingId")
        settUtfallForAlleVilkaar(vilkaarsvurdering) // Usikker på om vi må dette, hoppar over i første omgang

        retryOgPakkUt { settVilkaarsvurderingaSomHelhetSomOppfylt(behandlingId) }
    }

    private suspend fun opprettVilkaarsvurdering(behandlingId: UUID) =
        klient.post("$url/api/vilkaarsvurdering/$behandlingId/opprett") {
            contentType(ContentType.Application.Json)
        }

    private suspend fun settUtfallForAlleVilkaar(vilkaarsvurdering: HttpResponse) {
    }

    private suspend fun settVilkaarsvurderingaSomHelhetSomOppfylt(behandlingId: UUID) =
        klient.post("$url/api/vilkaarsvurdering/resultat/$behandlingId") {
            contentType(ContentType.Application.Json)
        }
}
