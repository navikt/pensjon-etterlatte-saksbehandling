package no.nav.etterlatte.grunnlag.aldersovergang

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.PersonRolle
import java.time.YearMonth

class AldersovergangService(private val dao: AldersovergangDao) {
    fun hentSoekereFoedtIEnGittMaaned(maaned: YearMonth) = dao.hentSoekereFoedtIEnGittMaaned(maaned)

    fun hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned: YearMonth) =
        dao.hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned)

    fun hentAlder(
        sakId: Long,
        rolle: PersonRolle,
    ): Alder? =
        when (rolle) {
            PersonRolle.BARN -> dao.hentAlder(sakId, Opplysningstype.SOEKER_PDL_V1)
            else -> throw NotImplementedError("Ikke støtta å finne alder per nå for rolle $rolle")
        }
}

typealias Alder = Int
