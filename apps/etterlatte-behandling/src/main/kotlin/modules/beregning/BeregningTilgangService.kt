package no.nav.etterlatte.modules.beregning

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.sak.TilgangServiceSjekker
import java.util.UUID

class BeregningTilgangService(
    private val tilgangService: TilgangServiceSjekker,
) : BehandlingTilgangsSjekk {
    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        val kontekst = Kontekst.get()
        val saksbehandler = kontekst.appUserAsSaksbehandler()
        if (skrivetilgang) {
            val enhet = kontekst.sakTilgangDao.hentSakMedGraderingOgSkjermingPaaBehandling(behandlingId)?.enhetNr
            if (enhet == null || enhet !in saksbehandler.hentEnheterMedSkrivetilgang()) {
                return false
            }
        }
        return tilgangService.harTilgangTilBehandling(behandlingId, saksbehandler.saksbehandlerMedRoller)
    }
}
