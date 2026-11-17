package no.nav.etterlatte.modules.beregning.grunnbeloep

import java.time.YearMonth

class GrunnbeloepService(
    private val repository: GrunnbeloepRepository,
) {
    fun hentGrunnbeloep(maaned: YearMonth) = repository.hentGjeldendeGrunnbeloep(maaned)
}
