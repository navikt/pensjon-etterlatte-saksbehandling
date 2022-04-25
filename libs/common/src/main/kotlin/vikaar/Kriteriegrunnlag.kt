package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning

data class Kriteriegrunnlag<T>(
    val kriterieOpplysningsType: KriterieOpplysningsType,
    val kilde: Behandlingsopplysning.Kilde,
    val opplysning: T
)

enum class KriterieOpplysningsType {
    ADRESSER,
    DOEDSDATO,
    FOEDSELSDATO,
    FORELDRE,
    AVDOED_UTENLANDSOPPHOLD,
    SOEKER_UTENLANDSOPPHOLD,
    BOSTEDADRESSE_SOEKER,
    BOSTEDADRESSE_GJENLEVENDE,
}