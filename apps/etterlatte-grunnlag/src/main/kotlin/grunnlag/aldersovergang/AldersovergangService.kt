package no.nav.etterlatte.grunnlag.aldersovergang

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate
import java.time.YearMonth

class AldersovergangService(
    private val dao: AldersovergangDao,
) {
    fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
    ): YearMonth? {
        val foedselsdato = dao.hentFoedselsdato(sakId, Opplysningstype.SOEKER_PDL_V1)
        return when (sakType) {
            SakType.BARNEPENSJON -> TODO()
            SakType.OMSTILLINGSSTOENAD -> foedselsdato?.let { YearMonth.from(it.plusYears(67)) }
        }
    }

    fun hentSoekereFoedtIEnGittMaaned(maaned: YearMonth) = dao.hentSoekereFoedtIEnGittMaaned(maaned)

    fun hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned: YearMonth) =
        dao.hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned)

    fun hentAlder(
        sakId: SakId,
        rolle: PersonRolle,
        paaDato: LocalDate,
    ): Alder? =
        when (rolle) {
            PersonRolle.BARN -> dao.hentFoedselsdato(sakId, Opplysningstype.SOEKER_PDL_V1)?.until(paaDato)?.years
            else -> throw NotImplementedError("Ikke støtta å finne alder per nå for rolle $rolle")
        }
}

typealias Alder = Int
