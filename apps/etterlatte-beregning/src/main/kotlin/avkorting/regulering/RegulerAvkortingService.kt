package no.nav.etterlatte.avkorting.regulering

import no.nav.etterlatte.beregning.Avkorting
import no.nav.etterlatte.beregning.AvkortingService
import no.nav.etterlatte.token.Bruker
import java.util.*

class RegulerAvkortingService(private val avkortingService: AvkortingService) {

    suspend fun regulerAvkorting(behandlignId: UUID, forrigeBehandlingId: UUID, bruker: Bruker): Avkorting {
        val forrigeAvkorting = avkortingService.hentAvkorting(forrigeBehandlingId) ?: throw Exception(
            "Fant ikke avkorting for $forrigeBehandlingId"
        )
        // TODO beregn restanse
        // TODO send liste med grunnlag
        return avkortingService.lagreAvkorting(behandlignId, bruker, forrigeAvkorting.avkortingGrunnlag[0])
    }
}