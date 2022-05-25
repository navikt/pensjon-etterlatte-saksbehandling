package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.person.PersonRolle

data class KommerSoekerTilgode(
    val kommerSoekerTilgodeVurdering: VilkaarResultat,
    val familieforhold: Familiemedlem,
)

data class Familiemedlem(
    val navn: String,
    val fnr: String,
    val rolle: PersonRolle,
    val adresser: Adresser
)