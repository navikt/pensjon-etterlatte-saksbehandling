package no.nav.etterlatte.samordning.sak

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.samordning.vedtak.getMeta
import org.slf4j.LoggerFactory

class BehandlingKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)
    private val behandlingUrl = "${config.getString("behandling.url")}/api"

    suspend fun hentSakForPerson(ident: FoedselsnummerDTO): List<SakId> {
        logger.info("Henter saker for person: ${ident.foedselsnummer.maskerFnr()}")
        return try {
            httpClient
                .post("$behandlingUrl/personer/getsak/oms") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(FoedselsnummerDTO(ident.foedselsnummer))
                }.body<List<SakId>>()
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod en feil i kall til behandling sak for personer")
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw BehandlingManglendeTilgang("Behandling: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw BehandlingUgyldigForespoersel("Behandling: Ugyldig forespørsel")
                else -> throw e
            }
        }
    }

    suspend fun hentSak(sakId: SakId): Sak? {
        logger.info("Henter sak med id=$sakId")

        return try {
            httpClient.get("$behandlingUrl/sak/${sakId.sakId}").body<Sak>()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                logger.info("Ingen sak med id=$sakId funnet")
                return null
            }

            logger.error("Henting av sak med id=$sakId feilet (status: ${e.response.status})")

            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw BehandlingManglendeTilgang("Behandling: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw BehandlingUgyldigForespoersel("Behandling: Ugyldig forespørsel")
                else -> throw e
            }
        }
    }
}

class BehandlingManglendeTilgang(
    detail: String,
) : IkkeFunnetException(
        code = "030-BEHANDLING-TILGANG",
        detail = detail,
        meta = getMeta(),
    )

class BehandlingUgyldigForespoersel(
    detail: String,
) : IkkeFunnetException(
        code = "031-BEHANDLING-FORESPOERSEL",
        detail = detail,
        meta = getMeta(),
    )
