package no.nav.etterlatte.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory

class GrunnlagKlient(
    config: Config,
    val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val baseUrl = config.getString("grunnlag.resource.url")

    internal suspend fun hentGrunnlagForSak(sakid: SakId): Grunnlag {
        try {
            logger.info("Henter grunnlag for sak med sakId=$sakid")
            return httpClient.get("$baseUrl/api/brev/sak/$sakid/opprett-journalfoer-og-distribuer").body<Grunnlag>()
        } catch (e: Exception) {
            logger.error("Henter grunnlag for sak med sakId=$sakid feilet", e)

            throw ForespoerselException(
                status = HttpStatusCode.InternalServerError.value,
                code = "KAN_IKKE_HENTE_GRUNNLAG_FOR_SAK",
                detail = "Kunne ikke hente grunnlag for sak: $sakid",
            )
        }
    }
}
