package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
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
    val adresser: Adresser,
    val foedselsdato: LocalDate?,
)

data class PersoninfoAvdoed(
    val navn: String,
    val fnr: Foedselsnummer?,
    val rolle: PersonRolle,
    val adresser: Adresser,
    val doedsdato: LocalDate?,
)


data class PersoninfoGjenlevendeForelder(
    val navn: String,
    val fnr: Foedselsnummer?,
    val rolle: PersonRolle,
    val adresser: Adresser,
    val adresseSoekand: String?,
)