package no.nav.etterlatte.no.nav.etterlatte.grunnbeloep

import no.nav.etterlatte.grunnbeloep.GrunnbeloepRepository
import java.time.YearMonth

class GrunnbeloepService(
    private val repository: GrunnbeloepRepository,
) {
    fun hentGrunnbeloep(maaned: YearMonth) = repository.hentGjeldendeGrunnbeloep(maaned)
}
