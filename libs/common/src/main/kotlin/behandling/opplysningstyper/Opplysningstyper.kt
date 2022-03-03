package no.nav.etterlatte.libs.common.behandling.opplysningstyper

enum class Opplysningstyper(val value: String) {
    GJENLEVENDE_FORELDER_PERSONINFO_V1("gjenlevende_forelder_personinfo:v1"),
    AVDOED_PERSONINFO_V1("avdoed_personinfo:v1"),
    AVDOED_DOEDSFALL_V1("avdoed_doedsfall:v1"),
    AVDOED_DOEDSAARSAK_V1("avdoed_doedsaarsak:v1"),
    AVDOED_UTENLANDSOPPHOLD_V1("avdoed_utenlandsopphold:v1"),
    AVDOED_INN_OG_UTFLYTTING_V1("avdoed_inn_og_utflytting:v1"),
    AVDOED_MILITAERTJENESTE_V1("avdoed_militaertjeneste:v1"),
    AVDOED_NAERINGSINNTEKT_V1("avdoed_naeringsinntekt:v1"),
    SOEKER_PERSONINFO_V1("soeker_personinfo:v1"),
    SOEKER_FOEDSELSDATO_V1("soeker_foedselsdato:v1"),
    SOEKER_RELASJON_FORELDRE_V1("soeker_relasjon_foreldre:v1"),
    SOEKER_RELASJON_SOESKEN_V1("soeker_relasjon_soesken:v1"),
    SOEKER_STATSBORGERSKAP_V1("soeker_statsborgerskap:v1"),
    SOEKER_UTENLANDSADRESSE_V1("soeker_utenlandsadresse:v1"),
    SOEKER_VERGE_V1("soeker_verge:v1"),
    SOEKER_DAGLIG_OMSORG_V1("soker_daglig_omsorg:v1"),
    INNSENDER_PERSONINFO_V1("innsender_personinfo:v1"),
    SOEKNAD_MOTTATT_DATO("soeknad_mottatt_dato"),
    UTBETALINGSINFORMASJON_V1("utbetalingsinformasjon:v1"),
    SAMTYKKE("samtykke"),
    TESTOPPLYSNING("testopplysning")
}