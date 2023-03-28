package no.nav.etterlatte.brev

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.adresse.enhetsregister.BrregService
import no.nav.etterlatte.brev.journalpost.AvsenderMottaker
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.tilgangssjekk.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.withBehandlingId
import org.slf4j.LoggerFactory

fun Route.brevRoute(service: BrevService, brregService: BrregService, behandlingKlient: BehandlingKlient) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.BrevRoute")

    route("brev") {
        get("maler") {
            val maler = listOf(
                Mal("Vedtak om innvilget barnepensjon", "innvilget"),
                Mal("Revurdert barnepensjon", "revurdering"),
                Mal("Dokumentasjon om vergemål", "verge")
            )

            call.respond(maler)
        }

        get("mottakere") {
            val statsforvaltere = brregService.hentAlleStatsforvaltere().map {
                AvsenderMottaker(it.organisasjonsnummer, idType = "ORGNR", it.navn)
            }

            // todo: Hent personer fra saksbehandlingen
            val personer = listOf(
                AvsenderMottaker("11057523044", idType = "FNR", navn = "Stor Snerk"),
                AvsenderMottaker("24116324268", idType = "FNR", navn = "Nobel Tøffeldyr")
            )

            call.respond(statsforvaltere + personer)
        }

        get("behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                call.respond(service.hentAlleBrev(behandlingId))
            }
        }

        post("behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val request = call.receive<OpprettBrevRequest>()

                val brevInnhold = service.opprett(request.mottaker, request.mal, request.enhet)
                val brev = service.lagreAnnetBrev(behandlingId, request.mottaker, brevInnhold)

                call.respond(brev)
            }
        }

        post("forhaandsvisning") {
            val request = call.receive<OpprettBrevRequest>()

            val brev = service.opprett(request.mottaker, request.mal, request.enhet)

            call.respond(brev.data)
        }

        post("pdf/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                try {
                    val mp = call.receiveMultipart().readAllParts()

                    val filData = mp.first { it is PartData.FormItem }
                        .let { objectMapper.readValue<FilData>((it as PartData.FormItem).value) }

                    val fil: ByteArray = mp.first { it is PartData.FileItem }
                        .let { (it as PartData.FileItem).streamProvider().readBytes() }

                    val brevInnhold = BrevInnhold(filData.filNavn, Spraak.NB, fil)

                    val brev = service.lagreAnnetBrev(behandlingId, filData.mottaker, brevInnhold)

                    call.respond(brev)
                } catch (e: Exception) {
                    logger.error("Getting multipart error", e)
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }

        post("{brevId}/pdf") {
            val brevId = call.parameters["brevId"]!!
            val innhold = service.hentBrevInnhold(brevId.toLong())

            call.respond(innhold.data)
        }

        delete("{brevId}") {
            val brevId = call.parameters["brevId"]!!

            val slettetOK = service.slettBrev(brevId.toLong())

            if (slettetOK) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("{brevId}/ferdigstill") {
            val brevId = call.parameters["brevId"]!!

            val ferdigstiltOK = service.ferdigstillBrev(brevId.toLong())

            if (ferdigstiltOK) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

data class Mal(
    val tittel: String,
    val navn: String
)

data class OpprettBrevRequest(
    val mal: Mal,
    val mottaker: Mottaker,
    val enhet: String
)

data class FilData(
    val mottaker: Mottaker,
    val filNavn: String
)