package no.nav.etterlatte.grunnlag.aldersovergang

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.PersonRolle
import java.time.LocalDate
import java.time.YearMonth

class AldersovergangService(
    private val dao: AldersovergangDao,
) {
    fun hentSoekereFoedtIEnGittMaaned(maaned: YearMonth) = dao.hentSoekereFoedtIEnGittMaaned(maaned)

    fun hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned: YearMonth) =
        dao.hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned)

    fun hentAlder(
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        rolle: PersonRolle,
        paaDato: LocalDate,
    ): Alder? =
        when (rolle) {
            PersonRolle.BARN -> dao.hentAlder(sakId, Opplysningstype.SOEKER_PDL_V1, paaDato)
            else -> throw NotImplementedError("Ikke støtta å finne alder per nå for rolle $rolle")
        }
}

typealias Alder = Int
