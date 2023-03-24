package no.nav.etterlatte.trygdetid

import java.util.*

class TrygdetidService(val trygdetidRepository: TrygdetidRepository) {

    // TODO Legg ti tilstandsjekk på samtlige

    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? = trygdetidRepository.hentTrygdetid(behandlingsId)

    fun opprettTrygdetid(behandlingsId: UUID): Trygdetid {
        trygdetidRepository.hentTrygdetid(behandlingsId)?.let {
            throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingsId")
        }
        return trygdetidRepository.opprettTrygdetid(behandlingsId)
    }

    fun lagreTrygdetidGrunnlag(behandlingsId: UUID, trygdetidGrunnlag: TrygdetidGrunnlag): Trygdetid =
        trygdetidRepository.opprettTrygdetidGrunnlag(behandlingsId, trygdetidGrunnlag)

    fun lagreBeregnetTrygdetid(behandlingsId: UUID, beregnetTrygdetid: BeregnetTrygdetid): Trygdetid =
        trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingsId, beregnetTrygdetid)
}