package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.UtenlandsadresseBarn
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import java.time.LocalDate

data class KommerSoekerTilgode(
    val kommerSoekerTilgodeVurdering: VilkaarResultat,
    val familieforhold: Familiemedlemmer,
)
data class Familiemedlemmer(
    val avdoed: PersoninfoAvdoed,
    val soeker: PersoninfoSoeker,
    val gjenlevendeForelder: PersoninfoGjenlevendeForelder
)

data class PersoninfoSoeker(
    val navn: String,
    val fnr: Foedselsnummer?,
    val rolle: PersonRolle,
    val bostedadresser: List<Adresse>?,
    val soeknadAdresse: UtenlandsadresseBarn?,
    val foedselsdato: LocalDate?,
)

data class PersoninfoAvdoed(
    val navn: String,
    val fnr: Foedselsnummer?,
    val rolle: PersonRolle,
    val bostedadresser: List<Adresse>?,
    val doedsdato: LocalDate?,
    val barn: List<Foedselsnummer>?,
)


data class PersoninfoGjenlevendeForelder(
    val navn: String,
    val fnr: Foedselsnummer?,
    val rolle: PersonRolle,
    val bostedadresser: List<Adresse>?,
)