package no.nav.etterlatte.trygdetid

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.trygdetid.avtale.AvtaleRepository
import java.util.UUID

data class HentTrygdetiderForAvdoedeRequest(
    val avdoede: List<String>,
)

fun Route.trygdetidCrudRoute(
    trygdetidRepository: TrygdetidRepository,
    avtaleRepository: AvtaleRepository,
) {
    route("/intern/trygdetid-crud") {
        route("/behandling/{behandlingId}") {
            get {
                val behandlingId = UUID.fromString(call.parameters["behandlingId"])
                call.respond(trygdetidRepository.hentTrygdetiderForBehandling(behandlingId))
            }

            get("/{trygdetidId}") {
                val behandlingId = UUID.fromString(call.parameters["behandlingId"])
                val trygdetidId = UUID.fromString(call.parameters["trygdetidId"])
                val trygdetid = trygdetidRepository.hentTrygdetidMedId(behandlingId, trygdetidId)
                if (trygdetid != null) {
                    call.respond(trygdetid)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        post("/opprett") {
            val trygdetid = call.receive<Trygdetid>()
            call.respond(trygdetidRepository.opprettTrygdetid(trygdetid))
        }

        put("/oppdater") {
            val trygdetid = call.receive<Trygdetid>()
            call.respond(trygdetidRepository.oppdaterTrygdetid(trygdetid))
        }

        delete("/{trygdetidId}") {
            val trygdetidId = UUID.fromString(call.parameters["trygdetidId"])
            trygdetidRepository.slettTrygdetid(trygdetidId)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/avdoede") {
            val request = call.receive<HentTrygdetiderForAvdoedeRequest>()
            call.respond(trygdetidRepository.hentTrygdetiderForAvdoede(request.avdoede))
        }

        route("/avtale") {
            get("/{behandlingId}") {
                val behandlingId = UUID.fromString(call.parameters["behandlingId"])
                val avtale = avtaleRepository.hentAvtale(behandlingId)
                if (avtale != null) {
                    call.respond(avtale)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/opprett") {
                val trygdeavtale = call.receive<Trygdeavtale>()
                avtaleRepository.opprettAvtale(trygdeavtale)
                call.respond(HttpStatusCode.Created)
            }

            put("/lagre") {
                val trygdeavtale = call.receive<Trygdeavtale>()
                avtaleRepository.lagreAvtale(trygdeavtale)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
