package no.nav.etterlatte.libs.common.vikaar


enum class Kriterietyper {
    AVDOED_ER_FORELDER,
    DOEDSFALL_ER_REGISTRERT_I_PDL,

    SOEKER_ER_I_LIVE,
    SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,

    AVDOED_IKKE_REGISTRERT_NOE_I_MEDL,
    AVDOED_NORSK_STATSBORGER,
    AVDOED_INGEN_INN_ELLER_UTVANDRING,
    AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR,
    AVDOED_KUN_NORSKE_BOSTEDSADRESSER,
    AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER,
    AVDOED_KUN_NORSKE_KONTAKTADRESSER,
    AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD,

    AVDOED_HAR_MOTTATT_PENSJON_SISTE_FEM_AAR,
    AVDOED_HAR_MOTTATT_TRYGD_SISTE_FEM_AAR,
    AVDOED_HAR_MOTTATT_PENSJON_TRYGD_SISTE_FEM_AAR,
    AVDOED_HAR_HATT_100PROSENT_STILLING_SISTE_FEM_AAR,

    SOEKER_IKKE_ADRESSE_I_UTLANDET,
    GJENLEVENDE_FORELDER_IKKE_ADRESSE_I_UTLANDET,
    SAMME_BOSTEDSADRESSE,
    SAKSBEHANDLER_RESULTAT,
}

enum class Metakriterietyper {
    AVDOED_MEDLEMSKAP_BOSTED
}
