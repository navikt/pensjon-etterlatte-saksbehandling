package no.nav.etterlatte.grunnlag.omregning

import java.time.YearMonth

class OmregningService(private val dao: OmregningDao) {
    fun hentSoekereFoedtIEnGittMaaned(maaned: YearMonth) = dao.hentSoekereFoedtIEnGittMaaned(maaned)
}
