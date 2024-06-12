package no.nav.etterlatte.brev.notat

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.brev.EtterlatteBrevKode
import no.nav.etterlatte.brev.NotatService
import no.nav.etterlatte.brev.NyNotatService
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.BrevbakerBlankettDTO
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.withSakId
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

        override fun tilLetterdata(): BrevbakerBlankettDTO = klage.tilBrevbakerBlankett()
    }
}

data class NotatRequest(
    val data: StrukturertBrev,
)

const val NOTAT_ID_CALL_PARAMETER = "notatId"

inline val PipelineContext<*, ApplicationCall>.notatId: NotatID
    get() =
        requireNotNull(call.parameters[NOTAT_ID_CALL_PARAMETER]?.toLong()) {
            "Gosys oppgaveId er ikke i path params"
        }

private val logger = LoggerFactory.getLogger("notatRoute")

fun Route.notatRoute(
    notatService: NotatService,
    nyNotatService: NyNotatService,
    tilgangsSjekk: Tilgangssjekker,
) {
    route("/notat") {
        route("/{$NOTAT_ID_CALL_PARAMETER}") {
            delete {
                nyNotatService.slett(notatId)
                call.respond(HttpStatusCode.OK)
            }

            get("/payload") {
                val payload = nyNotatService.hentPayload(notatId)
                call.respond(payload)
            }

            post("/payload") {
                val payload = call.receive<Slate>()

                nyNotatService.oppdaterPayload(notatId, payload, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }

            post("/tittel") {
                val tittel = call.request.queryParameters["tittel"]

                if (tittel.isNullOrBlank()) {
                    throw UgyldigForespoerselException(
                        code = "TITTEL_MANGLER",
                        detail = "Kan ikke oppdatere tittel når den er tom eller mangler!",
                    )
                } else {
                    nyNotatService.oppdaterTittel(notatId, tittel, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }

            get("/pdf") {
                val payload = nyNotatService.genererPdf(notatId)
                call.respond(payload)
            }

            post("/journalfoer") {
                nyNotatService.journalfoer(notatId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }
        }

        route("/sak/{$SAKID_CALL_PARAMETER}") {
            get {
                withSakId(tilgangsSjekk, skrivetilgang = true) { sakId ->
                    val notater = nyNotatService.hentForSak(sakId)
                    call.respond(notater)
                }
            }

            post {
                withSakId(tilgangsSjekk, skrivetilgang = true) { sakId ->
                    val mal = NotatMal.valueOf(call.request.queryParameters["mal"]!!)

                    val notat = nyNotatService.opprett(sakId, mal, brukerTokenInfo)

                    call.respond(notat)
                }
            }

            post("/forhaandsvis") {
                withSakId(tilgangsSjekk, skrivetilgang = false) { sakId ->
                    logger.info("Forhåndsviser notatpdf i sak $sakId")
                    medBody<NotatRequest> {
                        call.respond(notatService.forhaandsvisNotat(it.data, brukerTokenInfo).bytes)
                    }
                }
            }

            post("/journalfoer") {
                withSakId(tilgangsSjekk, skrivetilgang = true) { sakId ->
                    logger.info("Journalfører notat ")
                    medBody<NotatRequest> {
                        call.respond(notatService.journalfoerNotatISak(sakId, it.data, brukerTokenInfo))
                    }
                }
            }
        }
    }
}
