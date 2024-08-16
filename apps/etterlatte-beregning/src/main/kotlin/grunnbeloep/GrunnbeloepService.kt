package no.nav.etterlatte.grunnbeloep

import java.time.YearMonth

class GrunnbeloepService(
    private val repository: GrunnbeloepRepository,
) {
    fun hentGrunnbeloep(maaned: YearMonth) = repository.hentGjeldendeGrunnbeloep(maaned)
}
