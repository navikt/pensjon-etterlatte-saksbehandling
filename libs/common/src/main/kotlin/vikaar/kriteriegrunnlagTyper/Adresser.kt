package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.person.Adresse

data class Adresser(
    val bostedadresse: List<Adresse>?,
    val oppholdadresse: List<Adresse>?,
    val kontaktadresse: List<Adresse>?
)

data class Bostedadresser(
    val bostedadresse: List<Adresse>?,
)