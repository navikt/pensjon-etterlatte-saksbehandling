package barnepensjon.kommerbarnettilgode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.vikaar.Familiemedlemmer
import no.nav.etterlatte.libs.common.vikaar.PersoninfoAvdoed
import no.nav.etterlatte.libs.common.vikaar.PersoninfoGjenlevendeForelder
import no.nav.etterlatte.libs.common.vikaar.PersoninfoSoeker
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning

fun mapFamiliemedlemmer(
    soeker: Grunnlagsdata<JsonNode>?,
    soekerSoeknad: VilkaarOpplysning<SoekerBarnSoeknad>?,
    gjenlevende: Grunnlagsdata<JsonNode>?,
    avdoed: Grunnlagsdata<JsonNode>?
): Familiemedlemmer {
    return Familiemedlemmer(
        avdoed = avdoed?.let {
            PersoninfoAvdoed(
                navn = it.hentNavn()?.verdi.toString(),
                fnr = it.hentFoedselsnummer()?.verdi,
                rolle = PersonRolle.AVDOED,
                bostedadresser = it.hentBostedsadresse()?.verdi,
                doedsdato = it.hentDoedsdato()?.verdi,
                barn = it.hentAvdoedesbarn()?.verdi?.avdoedesBarn?.map { barn -> barn.foedselsnummer }
            )
        }!!,
        soeker = soeker?.let {
            PersoninfoSoeker(
                navn = it.hentNavn()?.verdi.toString(),
                fnr = it.hentFoedselsnummer()?.verdi,
                rolle = PersonRolle.BARN,
                bostedadresser = it.hentBostedsadresse()?.verdi,
                soeknadAdresse = soekerSoeknad?.opplysning?.utenlandsadresse,
                foedselsdato = it.hentFoedselsdato()?.verdi
            )
        }!!,
        gjenlevendeForelder = gjenlevende?.let {
            PersoninfoGjenlevendeForelder(
                navn = it.hentNavn()?.verdi.toString(),
                fnr = it.hentFoedselsnummer()?.verdi,
                rolle = PersonRolle.GJENLEVENDE,
                bostedadresser = it.hentBostedsadresse()?.verdi
            )
        }!!
    )
}