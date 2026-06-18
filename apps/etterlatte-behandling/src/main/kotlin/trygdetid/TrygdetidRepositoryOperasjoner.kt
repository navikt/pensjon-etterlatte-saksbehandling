package no.nav.etterlatte.trygdetid

import java.util.UUID

interface TrygdetidRepositoryOperasjoner {
    fun hentTrygdetidMedId(
        behandlingId: UUID,
        trygdetidId: UUID,
    ): Trygdetid?

    fun hentTrygdetid(behandlingId: UUID): Trygdetid?

    fun hentTrygdetiderForBehandling(behandlingId: UUID): List<Trygdetid>

    fun opprettTrygdetid(trygdetid: Trygdetid): Trygdetid

    fun oppdaterTrygdetid(oppdatertTrygdetid: Trygdetid): Trygdetid

    fun slettTrygdetid(trygdetidId: UUID)

    fun hentTrygdetiderForAvdoede(avdoede: List<String>): List<TrygdetidPartial>
}
