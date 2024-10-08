package no.nav.etterlatte.grunnlag.aldersovergang

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
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
            SakType.BARNEPENSJON -> throw UgyldigForespoerselException(
                "SAKTYPE_IKKE_STØTTET",
                "Uthenting av måned for opphør grunnet aldersovergang støttes ikke for barnepensjon",
            )
            // Mottaker av Omstillingstønad opphører måned etter fylt 67 (§ 22-12 sjette ledd)
            SakType.OMSTILLINGSSTOENAD -> foedselsdato?.let { YearMonth.from(it.plusYears(67).plusMonths(1)) }
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
