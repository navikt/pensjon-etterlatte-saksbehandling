package no.nav.etterlatte.behandling.etteroppgjoer

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import java.util.UUID

fun Route.etteroppgjoerRoutes(service: EtteroppgjoerService) {
    route("/api/etteroppgjoer/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            val etteroppgjoer = service.hentEtteroppgjoer(behandlingId)
            call.respond(etteroppgjoer)
        }
    }
}

data class Etteroppgjoer(
    val behandling: EtteroppgjoerBehandling,
    val opplysninger: EtteroppgjoerOpplysninger,
)

data class EtteroppgjoerBehandling(
    val id: UUID,
    val status: String, // TODO enum
    val sak: Sak,
    val aar: Int,
    val opprettet: Tidspunkt,
)

data class EtteroppgjoerOpplysninger(
    val skatt: OpplysnignerSkatt,
    val ainntekt: AInntekt,
    // TODO..
)

data class OpplysnignerSkatt(
    val aarsinntekt: Int,
)

data class AInntekt(
    val inntektsmaaneder: List<AInntektMaaned>,
)

data class AInntektMaaned(
    val maaned: String,
    val summertBeloep: Int,
)
