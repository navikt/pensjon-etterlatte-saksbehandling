package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.*

data class Kriteriegrunnlag<T>(
    val id: UUID,
    val kriterieOpplysningsType: KriterieOpplysningsType,
    val kilde: Grunnlagsopplysning.Kilde,
    val opplysning: T
)

enum class KriterieOpplysningsType {
    ADRESSER,
    DOEDSDATO,
    FOEDSELSDATO,
    FORELDRE,
    AVDOED_UFORE_PENSJON,
    AVDOED_UTENLANDSOPPHOLD,
    SOEKER_UTENLANDSOPPHOLD,
    BOSTEDADRESSE_SOEKER,
    BOSTEDADRESSE_GJENLEVENDE,
    BOSTEDADRESSE_AVDOED,
}