package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle

data class KommerSoekerTilgode(
    val kommerSoekerTilgodeVurdering: VilkaarResultat,
    val familieforhold: List<Familiemedlem?>,
)

data class Familiemedlem(
    val navn: String,
    val fnr: Foedselsnummer,
    val rolle: PersonRolle,
    val adresser: Adresser
)