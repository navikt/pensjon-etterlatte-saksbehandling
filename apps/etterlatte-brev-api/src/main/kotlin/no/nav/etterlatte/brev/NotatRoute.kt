package no.nav.etterlatte.brev

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.BrevbakerBlankettDTO
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.withSakId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class StrukturertBrev {
    abstract val brevkode: EtterlatteBrevKode
    abstract val sak: Sak
    abstract val soekerFnr: String
    open val behandlingId: UUID? = null
    open val spraak = Spraak.NB

    abstract fun tilLetterdata(): Any

    @JsonTypeName("KLAGE_BLANKETT")
    data class KlageBlankett(
        val klage: Klage,
    ) : StrukturertBrev() {
        override val brevkode: EtterlatteBrevKode = EtterlatteBrevKode.KLAGE_OVERSENDELSE_BLANKETT
        override val sak: Sak = klage.sak
        override val soekerFnr: String = klage.sak.ident
        override val behandlingId = klage.id

        override fun tilLetterdata(): BrevbakerBlankettDTO {
            return klage.tilBrevbakerBlankett()
        }
    }
}

data class NotatRequest(
    val data: StrukturertBrev,
)

const val NOTAT_ID_CALL_PARAMETER = "notatId"
private val logger = LoggerFactory.getLogger("notatRoute")

fun Route.notatRoute(
    notatService: NotatService,
    tilgangsSjekk: Tilgangssjekker,
) {
    route("notat/{$SAKID_CALL_PARAMETER}") {
        post("forhaandsvis") {
            withSakId(tilgangsSjekk, skrivetilgang = false) { sakId ->
                logger.info("Forhåndsviser notatpdf i sak $sakId")
                medBody<NotatRequest> {
                    call.respond(notatService.forhaandsvisNotat(it.data, brukerTokenInfo))
                }
            }
        }

        get("{$NOTAT_ID_CALL_PARAMETER}") {
            val notatId =
                call.parameters[NOTAT_ID_CALL_PARAMETER]?.toLong()
                    ?: throw UgyldigForespoerselException("MISSING_NOTAT_ID", "Mangler notatid")

            withSakId(tilgangsSjekk, skrivetilgang = false) {
                logger.info("Henter pdf for notat med id=$notatId i sak $it")
                call.respond(notatService.hentPdf(notatId))
            }
        }

        post("journalfoer") {
            withSakId(tilgangsSjekk, skrivetilgang = true) { sakId ->
                logger.info("Journalfører notat ")
                medBody<NotatRequest> {
                    call.respond(notatService.journalfoerNotatISak(sakId, it.data, brukerTokenInfo))
                }
            }
        }
    }
}
