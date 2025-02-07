package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyePersonopplysninger
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory
import java.util.UUID

class GrunnlagKlient(
    private val klient: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    fun lagreNyePersonopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        logger.info("Lagrer nye personopplysninger på sak $sakId")

        runBlocking {
            klient.post("$url/grunnlag/person/behandling/$behandlingId/nye-opplysninger") {
                contentType(ContentType.Application.Json)
                setBody(NyePersonopplysninger(sakId, fnr, nyeOpplysninger))
            }
        }
    }

    fun lagreNyeSaksopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        logger.info("Lagrer nye saksopplysninger på sak $sakId")

        runBlocking {
            klient.post("$url/grunnlag/behandling/$behandlingId/nye-opplysninger") {
                contentType(ContentType.Application.Json)
                setBody(NyeSaksopplysninger(sakId, nyeOpplysninger))
            }
        }
    }

    fun laasVersjonForBehandling(behandlingId: UUID) {
        logger.info("Låser grunnlagsversjon for behandling (id=$behandlingId)")

        runBlocking {
            klient.post("$url/grunnlag/behandling/$behandlingId/laas") {
                contentType(ContentType.Application.Json)
            }
        }
    }
}
