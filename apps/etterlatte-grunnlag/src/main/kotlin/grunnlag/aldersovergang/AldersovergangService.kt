package no.nav.etterlatte.grunnlag.aldersovergang

import java.time.YearMonth

class AldersovergangService(private val dao: AldersovergangDao) {
    fun hentSoekereFoedtIEnGittMaaned(maaned: YearMonth) = dao.hentSoekereFoedtIEnGittMaaned(maaned)

    fun hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned: YearMonth) =
        dao.hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned)
}
