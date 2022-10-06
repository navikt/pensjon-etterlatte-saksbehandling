package barnepensjon.kommerbarnettilgode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentAvdoedesbarn
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.hentUtenlandsadresse
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.vikaar.Familiemedlemmer
import no.nav.etterlatte.libs.common.vikaar.PersoninfoAvdoed
import no.nav.etterlatte.libs.common.vikaar.PersoninfoGjenlevendeForelder
import no.nav.etterlatte.libs.common.vikaar.PersoninfoSoeker

fun mapFamiliemedlemmer(
    soeker: Grunnlagsdata<JsonNode>?,
    gjenlevende: Grunnlagsdata<JsonNode>?,
    avdoed: Grunnlagsdata<JsonNode>?
): Familiemedlemmer {
    return Familiemedlemmer(
        avdoed = avdoed?.let {
            PersoninfoAvdoed(
                navn = it.hentNavn()?.verdi.toString(),
                fnr = it.hentFoedselsnummer()?.verdi,
                rolle = PersonRolle.AVDOED,
                bostedadresser = it.hentBostedsadresse()?.perioder?.map { it.verdi },
                doedsdato = it.hentDoedsdato()?.verdi,
                barn = it.hentAvdoedesbarn()?.verdi?.avdoedesBarn?.map { barn -> barn.foedselsnummer }
            )
        }!!,
        soeker = soeker?.let {
            PersoninfoSoeker(
                navn = it.hentNavn()?.verdi.toString(),
                fnr = it.hentFoedselsnummer()?.verdi,
                rolle = PersonRolle.BARN,
                bostedadresser = it.hentBostedsadresse()?.perioder?.map { it.verdi },
                soeknadAdresse = UtenlandsadresseBarn(
                    adresseIUtlandet = it.hentUtenlandsadresse()?.verdi?.harHattUtenlandsopphold,
                    land = it.hentUtenlandsadresse()?.verdi?.land,
                    adresse = it.hentUtenlandsadresse()?.verdi?.adresse
                ),
                foedselsdato = it.hentFoedselsdato()?.verdi
            )
        }!!,
        gjenlevendeForelder = gjenlevende?.let {
            PersoninfoGjenlevendeForelder(
                navn = it.hentNavn()?.verdi.toString(),
                fnr = it.hentFoedselsnummer()?.verdi,
                rolle = PersonRolle.GJENLEVENDE,
                bostedadresser = it.hentBostedsadresse()?.perioder?.map { it.verdi }
            )
        }!!
    )
}