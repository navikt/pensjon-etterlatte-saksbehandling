package no.nav.etterlatte.brev.varselbrev

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.hentinformasjon.Tilgangssjekker
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.withBehandlingId

internal fun Route.varselbrevRoute(
    service: VarselbrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    route("brev/behandling/{$BEHANDLINGID_CALL_PARAMETER}/varsel") {
        get {
            withBehandlingId(tilgangssjekker) {
                service.hentVarselbrev(behandlingId)
            }
        }
    }
}
