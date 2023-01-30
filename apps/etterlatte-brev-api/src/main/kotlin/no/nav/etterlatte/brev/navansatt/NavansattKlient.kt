package no.nav.etterlatte.brev.navansatt

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import no.nav.etterlatte.brev.adresse.AdresseException
import no.nav.etterlatte.brev.model.SaksbehandlerInfo
import no.nav.etterlatte.libs.common.logging.getXCorrelationId
import org.slf4j.LoggerFactory

class NavansattKlient(
    private val client: HttpClient,
    private val url: String
) {
    private val logger = LoggerFactory.getLogger(NavansattKlient::class.java)

    suspend fun hentSaksbehandlerInfo(ident: String): SaksbehandlerInfo = try {
        logger.info("Henter informasjon om saksbehandler")
        client.get("$url/navansatt/$ident") {
            header("x_correlation_id", getXCorrelationId())
            header("Nav_Call_Id", getXCorrelationId())
        }.body()
    } catch (exception: Exception) {
        throw AdresseException("Feil i kall mot navansatt", exception)
    }
}