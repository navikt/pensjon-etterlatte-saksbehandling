package no.nav.etterlatte.trygdetid.regulering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.trygdetid.Trygdetid
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import java.util.*

class RegulerTrygdetidService(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService
) {

    suspend fun regulerTrygdetid(behandlingId: UUID, forrigeBehandlingId: UUID, bruker: Bruker): Trygdetid {
        val forrigeTrygdetid = trygdetidRepository.hentTrygdetid(forrigeBehandlingId)
        val forrigeTrygdetidGrunnlag =
            forrigeTrygdetid?.trygdetidGrunnlag ?: throw Exception("Mangler trygdetid for $forrigeBehandlingId")
        val regulering = behandlingKlient.hentBehandling(behandlingId, bruker)

        return trygdetidRepository.transaction { tx ->

            trygdetidRepository.opprettTrygdetid(regulering, forrigeTrygdetid.opplysninger, tx)
            forrigeTrygdetidGrunnlag.forEach { grunnlag ->
                trygdetidRepository.opprettTrygdetidGrunnlag(behandlingId, grunnlag, tx)
            }
            val beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetid(forrigeTrygdetidGrunnlag)

            trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid, tx).also {
                runBlocking { behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker) }
            }
        }
    }
}