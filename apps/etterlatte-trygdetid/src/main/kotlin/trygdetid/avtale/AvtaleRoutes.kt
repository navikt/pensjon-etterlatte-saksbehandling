package no.nav.etterlatte.trygdetid.avtale

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import java.util.UUID

fun Route.avtale(
    avtaleService: AvtaleService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/trygdetid/avtaler") {
        val logger = application.log

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
                withBehandlingId(behandlingKlient) {
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
    kilde = Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now()),
)
