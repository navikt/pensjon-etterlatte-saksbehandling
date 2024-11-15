package no.nav.etterlatte.trygdetid.avtale

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import org.slf4j.LoggerFactory
import java.util.UUID

fun Route.avtale(
    avtaleService: AvtaleService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/trygdetid/avtaler") {
        val logger = LoggerFactory.getLogger("AvtaleRoute")

        get {
            logger.info("Henter alle avtaler")
            call.respond(avtaleService.hentAvtaler())
        }

        get("/kriteria") {
            logger.info("Henter alle avtalekriterier")
            call.respond(avtaleService.hentAvtaleKriterier())
        }

        route("{$BEHANDLINGID_CALL_PARAMETER}") {
            get {
                withBehandlingId(behandlingKlient) {
                    logger.info("Henter trygdeavtale for behandling $behandlingId")
                    val avtale = avtaleService.hentAvtaleForBehandling(behandlingId)
                    if (avtale != null) {
                        call.respond(avtale)
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }

            post {
                withBehandlingId(behandlingKlient, skrivetilgang = true) {
                    logger.info("Lagrer trygdeavtale for behandling $behandlingId")

                    val trygdeavtaleRequest = call.receive<TrygdeavtaleRequest>()
                    val avtale = trygdeavtaleRequest.toTrygdeavtale(behandlingId, brukerTokenInfo)

                    when (trygdeavtaleRequest.id) {
                        null -> avtaleService.opprettAvtale(avtale)
                        else -> avtaleService.lagreAvtale(avtale)
                    }

                    call.respond(avtale)
                }
            }
        }
    }
}

private fun TrygdeavtaleRequest.toTrygdeavtale(
    behandlingId: UUID,
    brukerTokenInfo: BrukerTokenInfo,
) = Trygdeavtale(
    id = id ?: UUID.randomUUID(),
    behandlingId = behandlingId,
    avtaleKode = avtaleKode,
    avtaleDatoKode = avtaleDatoKode,
    avtaleKriteriaKode = avtaleKriteriaKode,
    personKrets = personKrets,
    arbInntekt1G = arbInntekt1G,
    arbInntekt1GKommentar = arbInntekt1GKommentar,
    beregArt50 = beregArt50,
    beregArt50Kommentar = beregArt50Kommentar,
    nordiskTrygdeAvtale = nordiskTrygdeAvtale,
    nordiskTrygdeAvtaleKommentar = nordiskTrygdeAvtaleKommentar,
    kilde = Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now()),
)
