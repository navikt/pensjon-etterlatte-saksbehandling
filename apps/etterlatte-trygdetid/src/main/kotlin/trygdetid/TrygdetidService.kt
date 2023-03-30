package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import java.util.*

class TrygdetidService(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient
) {
    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? = trygdetidRepository.hentTrygdetid(behandlingsId)

    suspend fun opprettTrygdetid(behandlingId: UUID, bruker: Bruker): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            trygdetidRepository.hentTrygdetid(behandlingId)?.let {
                throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingId")
            }
            trygdetidRepository.opprettTrygdetid(behandlingId)
        }

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            trygdetidRepository.opprettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag)
        }

    suspend fun lagreBeregnetTrygdetid(
        behandlingId: UUID,
        bruker: Bruker,
        beregnetTrygdetid: BeregnetTrygdetid
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)
        }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Trygdetid): Trygdetid {
        val kanFastsetteTrygdetid = behandlingKlient.kanBeregnes(behandlingId, bruker, false)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }
}