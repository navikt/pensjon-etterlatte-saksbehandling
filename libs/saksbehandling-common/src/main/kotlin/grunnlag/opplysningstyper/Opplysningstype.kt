package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

enum class Opplysningstype {
    PERSONGALLERI_V1,
    PERSONGALLERI_PDL_V1,

    AVDOED_PDL_V1,
    GJENLEVENDE_FORELDER_PDL_V1,
    SOEKER_PDL_V1,
    INNSENDER_PDL_V1,

    AVDOED_SOEKNAD_V1,
    SOEKER_SOEKNAD_V1,
    GJENLEVENDE_FORELDER_SOEKNAD_V1,
    INNSENDER_SOEKNAD_V1,

    UTBETALINGSINFORMASJON_V1,
    SOEKNAD_MOTTATT_DATO,
    SAMTYKKE,
    SPRAAK,
    SOEKNADSTYPE_V1,

    // Grunnlag v2
    NAVN,
    FOEDSELSNUMMER,
    FOEDSELSDATO,
    FOEDSELSAAR,
    FOEDELAND,
    DOEDSDATO,
    ADRESSEBESKYTTELSE,
    BOSTEDSADRESSE,
    DELTBOSTEDSADRESSE,
    KONTAKTADRESSE,
    OPPHOLDSADRESSE,
    SIVILSTATUS,
    SIVILSTAND,
    STATSBORGERSKAP,
    UTLAND,
    FAMILIERELASJON,
    AVDOEDESBARN,
    VERGEMAALELLERFREMTIDSFULLMAKT,
    PERSONROLLE,
    UTENLANDSOPPHOLD, // Bruks kun idag for informasjon fra søknaden
    UTENLANDSADRESSE,

    SOESKEN_I_BEREGNINGEN, // Kun for soeskenjustering

    HISTORISK_FORELDREANSVAR,

    @Deprecated("Finnes bare i gamle rader i DB")
    VERGES_ADRESSE,
    FORELDRELOES,
    UFOERE,
}
