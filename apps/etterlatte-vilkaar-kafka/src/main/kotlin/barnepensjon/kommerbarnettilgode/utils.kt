package barnepensjon.kommerbarnettilgode

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.vikaar.Familiemedlemmer
import no.nav.etterlatte.libs.common.vikaar.PersoninfoAvdoed
import no.nav.etterlatte.libs.common.vikaar.PersoninfoGjenlevendeForelder
import no.nav.etterlatte.libs.common.vikaar.PersoninfoSoeker
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning

fun mapFamiliemedlemmer(
    soeker: VilkaarOpplysning<Person>?,
    soekerSoeknad: VilkaarOpplysning<SoekerBarnSoeknad>?,
    gjenlevende: VilkaarOpplysning<Person>?,
    avdoed: VilkaarOpplysning<Person>?
): Familiemedlemmer {
    return Familiemedlemmer(
        avdoed = avdoed?.opplysning.let {
            PersoninfoAvdoed(
                navn = it?.fornavn + " " + it?.etternavn,
                fnr = it?.foedselsnummer,
                rolle = PersonRolle.AVDOED,
                bostedadresser = it?.bostedsadresse,
                doedsdato = it?.doedsdato,
                barn = it?.familieRelasjon?.barn
            )
        },
        soeker = soeker?.opplysning.let {
            PersoninfoSoeker(
                navn = it?.fornavn + " " + it?.etternavn,
                fnr = it?.foedselsnummer,
                rolle = PersonRolle.BARN,
                bostedadresser = it?.bostedsadresse,
                soeknadAdresse = soekerSoeknad?.opplysning?.utenlandsadresse,
                foedselsdato = it?.foedselsdato
            )
        },
        gjenlevendeForelder = gjenlevende?.opplysning.let {
            PersoninfoGjenlevendeForelder(
                navn = it?.fornavn + " " + it?.etternavn,
                fnr = it?.foedselsnummer,
                rolle = PersonRolle.GJENLEVENDE,
                bostedadresser = it?.bostedsadresse
            )
        }
    )
}