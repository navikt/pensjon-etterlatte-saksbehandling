package no.nav.etterlatte.libs.common.behandling.opplysningstyper

enum class Opplysningstyper(val value: String) {
    GJENLEVENDE_FORELDER_PERSONINFO_V1("gjenlevende_forelder_personinfo:v1"),
    AVDOED_PERSONINFO_V1("avdoed_personinfo:v1"),
    AVDOED_DOEDSFALL_V1("avdoed_doedsfall:v1"),
    AVDOED_DOEDSAARSAK_V1("avdoed_doedsaarsak:v1"),
    AVDOED_UTENLANDSOPPHOLD_V1("avdoed_utenlandsopphold:v1"),
    SOEKER_PERSONINFO_V1("soeker_personinfo:v1"),
    SOEKER_FOEDSELSDATO_V1("soeker_foedselsdato:v1"),
    SOEKER_RELASJON_FORELDRE_V1("soeker_relasjon_foreldre:v1"),
    SOEKER_RELASJON_SOESKEN_V1("soeker_relasjon_soesken:v1"),
    SOEKNAD_MOTTATT_DATO("soeknad_mottatt_dato")
}