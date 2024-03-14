package no.nav.etterlatte.behandling.omregning

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import java.util.UUID

fun Route.omregningRoutes(omregningService: OmregningService) {
    route("/omregning") {
        post {
            val request = call.receive<Omregningshendelse>()
            kunSkrivetilgang(sakId = request.sakId) {
                val forrigeBehandling = inTransaction { omregningService.hentForrigeBehandling(request.sakId) }
                val persongalleri = omregningService.hentPersongalleri(forrigeBehandling.id)
                val revurderingOgOppfoelging =
                    inTransaction {
                        omregningService.opprettOmregning(
                            sakId = request.sakId,
                            fraDato = request.fradato,
                            prosessType = request.prosesstype,
                            forrigeBehandling = forrigeBehandling,
                            persongalleri = persongalleri,
                        )
                    }
                retryOgPakkUt { revurderingOgOppfoelging.leggInnGrunnlag() }
                retryOgPakkUt {
                    inTransaction {
                        revurderingOgOppfoelging.opprettOgTildelOppgave()
                    }
                }
                retryOgPakkUt { revurderingOgOppfoelging.sendMeldingForHendelse() }
                val behandlingId = revurderingOgOppfoelging.behandlingId()
                val sakType = revurderingOgOppfoelging.sakType()
                call.respond(OpprettOmregningResponse(behandlingId, forrigeBehandling.id, sakType))
            }
        }
    }
}

data class OpprettOmregningResponse(val behandlingId: UUID, val forrigeBehandlingId: UUID, val sakType: SakType)
