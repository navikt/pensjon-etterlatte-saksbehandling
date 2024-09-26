package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Person
import java.util.UUID

data class Personopplysning(
    val opplysningType: Opplysningstype,
    val id: UUID,
    val kilde: GenerellKilde,
    val opplysning: Person,
)
