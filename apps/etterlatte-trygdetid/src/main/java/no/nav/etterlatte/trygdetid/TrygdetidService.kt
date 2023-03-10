package no.nav.etterlatte.trygdetid

class TrygdetidService(val trygdetidRepository: TrygdetidRepository) {

    fun hentTrygdetid(): Trygdetid {
        val trygdetid = trygdetidRepository.hentTrygdetid()
        return trygdetid
    }

    fun lagreTrygdetidGrunnlag(trygdetidGrunnlag: TrygdetidGrunnlag) {
        trygdetidRepository.lagreTrygdetidGrunnlag(trygdetidGrunnlag)
    }
}