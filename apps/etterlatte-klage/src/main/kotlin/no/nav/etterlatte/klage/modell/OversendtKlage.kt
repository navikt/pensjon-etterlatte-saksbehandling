package no.nav.etterlatte.klage.modell

import no.nav.etterlatte.libs.common.Vedtaksloesning
import java.time.LocalDate

// objektet som skal sendes til kabal
data class OversendtKlageAnkeV3(
    val type: Type,
    val klager: OversendtKlager,
    val sakenGjelder: OversendtSakenGjelder? = null,
    val fagsak: OversendtSak? = null,
    val kildeReferanse: String,
    val dvhReferanse: String? = null,
    val innsynUrl: String? = null, // url til vår løsning
    val hjemler: List<KabalHjemmel>,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<OversendtDokumentReferanse> = emptyList(),
    val brukersHenvendelseMottattNavDato: LocalDate,
    val innsendtTilNav: LocalDate,
    val kilde: Vedtaksloesning,
    val ytelse: Ytelse,
    val kommentar: String? = null
)

enum class Type(override val id: String, override val navn: String, override val beskrivelse: String) : Kode {
    KLAGE("1", "Klage", "Klage"),
    ANKE("2", "Anke", "Anke")
}

enum class OversendtPartIdType {
    PERSON,
    VIRKSOMHET
}

data class OversendtPartId(
    val type: OversendtPartIdType,
    val verdi: String
)

data class OversendtKlager(
    val id: OversendtPartId,
    val klagersProsessfullmektig: OversendtProsessfullmektig? = null
)

data class OversendtProsessfullmektig(
    val id: OversendtPartId,
    val skalKlagerMottaKopi: Boolean
)

data class OversendtSakenGjelder(
    val id: OversendtPartId,
    val skalMottaKopi: Boolean
)

data class OversendtSak(
    val fagsakId: String? = null,
    val fagsystem: Vedtaksloesning
)

data class OversendtDokumentReferanse(
    val type: MottakDokumentType
)

enum class MottakDokumentType {
    BRUKERS_SOEKNAD,
    OPPRINNELIG_VEDTAK,
    BRUKERS_KLAGE,
    BRUKERS_ANKE,
    OVERSENDELSESBREV,
    KLAGE_VEDTAK,
    ANNET
}

enum class Ytelse {
    ENF_ENF,
    BAR_BAR,
    KON_KON
}

interface Kode {
    val id: String
    val navn: String
    val beskrivelse: String
}

enum class LovKilde(override val id: String, override val navn: String, override val beskrivelse: String) : Kode {
    FOLKETRYGDLOVEN("1", "Folketrygdloven", "Ftrl"),
    FORSKRIFT_OM_AKTIVITETSHJELPEMIDLER_TIL_DE_OVER_26_ÅR(
        "2",
        "Forskrift om aktivitetshjelpemidler til de over 26 år",
        "Forskrift om aktivitetshjelpemidler til de over 26 år"
    ),
    FORSKRIFT_OM_HJELPEMIDLER_MM("3", "Forskrift om hjelpemidler mm.", "Forskrift om hjelpemidler mm."),
    FORSKRIFT_OM_ORTOPEDISKE_HJELPEMIDLER_MM(
        "4",
        "Forskrift om ortopediske hjelpemidler mm.",
        "Forskrift om ortopediske hjelpemidler mm."
    ),
    FORSKRIFT_OM_HØREAPPARATER_MM("5", "Forskrift om høreapparater mm.", "Forskrift om høreapparater mm."),
    FORSKRIFT_OM_SERVICEHUND("6", "Forskrift om servicehund", "Forskrift om servicehund"),
    FORSKRIFT_OM_MOTORKJØRETØY("7", "Forskrift om motorkjøretøy", "Forskrift om motorkjøretøy"),
    FORVALTNINGSLOVEN("8", "Forvaltningsloven", "Fvl"),
    TRYGDERETTSLOVEN("9", "Trygderettsloven", "Trrl"),
    FORELDELSESLOVEN("10", "Foreldelsesloven", "Fl"),
    EØS_FORORDNING_883_2004("11", "EØS forordning 883/2004", "EØS forordning 883/2004"),
    DAGPENGEFORSKRIFTEN("12", "Dagpengeforskriften", "Dagpengeforskriften"),
    FORSKRIFT_COVID_19("13", "Forskrift Covid-19", "Forskrift Covid-19"),
    PERMITTERINGSLØNNSLOVEN("14", "Permitteringslønnsloven", "Pmll"),
    NORDISK_KONVENSJON("15", "Nordisk konvensjon", "Nordisk konvensjon"),
    GJENNOMFØRINGSFORORDNING_987_2009("16", "Gjennomføringsforordning 987/2009", "Gjennomføringsforordning 987/2009"),
    FORSKRIFT_UFØRETRYGD_FOLKETRYGDEN("17", "Forskrift om uføretrygd fra folketrygden", "Fors uføretr"),
    FORSKRIFT_OM_BEREGNING_AV_UFØRETRYGD_ETTER_EØS_AVTALEN_883_2004(
        "18",
        "Forskrift om beregning av uføretrygd etter EØS- avtalen 883/2004",
        "Forskrift om beregning av uføretrygd etter EØS- avtalen 883/2004"
    ),
    GJENNOMFØRINGSFORORDNING_987_2007("19", "Gjennomføringsforordning 987/2007", "Gjennomføringsforordning 987/2007"),
    EØS_FORORDNING_1408_71("20", "Forordning 1408/71", "Forordning 1408/71"),
    ANDRE_TRYGDEAVTALER("21", "Andre trygdeavtaler", "Andre trygdeavtaler"),
    GAMMELT_REGELVERK("22", "Gammelt regelverk", "Gammelt regelverk"),
    TRYGDEAVTALER_MED_ENGLAND("23", "Trygdeavtaler med England", "Trygdeavtaler med England"),
    TRYGDEAVTALER_MED_USA("24", "Trygdeavtaler med USA", "Trygdeavtaler med USA"),
    LOV_OM_KRIGSPENSJON_FOR_MILITÆRPERSONER(
        "25",
        "Lov om krigspensjon for militærpersoner",
        "Lov om krigspensjon for militærpersoner"
    ),
    LOV_OM_KRIGSPENSJON_FOR_SIVILE("26", "Lov om krigspensjon for sivile m.v", "Lov om krigspensjon for sivile m.v"),
    TILLEGGSLOV_OM_KRIGSPENSJONERING_AV_1951(
        "27",
        "Tilleggslov om krigspensjonering av 1951",
        "Tilleggslov om krigspensjonering av 1951"
    ),
    TILLEGGSLOV_OM_KRIGSPENSJONERING_AV_1968(
        "28",
        "Tilleggslov om krigspensjonering av 1968",
        "Tilleggslov om krigspensjonering av 1968"
    ),
    BARNETRYGDLOVEN("29", "Barnetrygdloven", "Btrl"),
    EØS_AVTALEN("30", "EØS-avtalen", "EØS-avtalen"),
    KONTANTSTØTTELOVEN("31", "Kontantstøtteloven", "Kontsl"),
    BARNELOVEN("32", "Barneloven", "Bl"),
    FORSKRIFT_OM_FASTSETTELSE_OG_ENDRING_AV_FORSTRINGSTILSKOT(
        "33",
        "Forskrift om fastsettelse og endring av fostringstilskot",
        "Fors fasts og end"
    ),
    FORSKRIFT_OM_SÆRTILSKUDD("34", "Forskrift om særtilskudd", "Fors. om sært."),
    FORSKOTTERINGSLOVEN("35", "Forskotteringsloven", "Forskl"),
    BIDRAGSINNKREVINGSLOVEN("36", "Bidragsinnkrevingsloven", "Innkl"),
    EKTESKAPSLOVEN("37", "Ekteskapsloven", "El"),
    BARNEVERNLOVEN("38", "Barnevernloven", "Bvl"),
    LOV_OM_SUPPLERENDE_STØNAD("39", "Lov om supplerende stønad", "Lov om supplerende stønad"),
    ARBEIDSMARKEDSLOVEN("40", "Arbeidsmarkedsloven", "Arbml"),
    FORSKRIFT_OM_TILTAK("41", "Forskrift om tiltak", "Forskrift om tiltak"),
    FORSKRIFT_OM_TILTAKSPENGER("41", "Forskrift om tiltakspenger", "Fors om tilt.p."),
    LØNNSGARANTILOVEN("42", "Lønnsgarantiloven", "Lgl"),
    AFP_62_PRIVAT("43", "AFP-62 Privat", "AFP-62 Privat"),
    AFP_62_OFFENTLIG("44", "AFP-62 Offentlig", "AFP-62 Offentlig"),
    FORSKRIFT_OM_ARBEIDS_OG_UTDANNINGSREISER(
        "45",
        "Forskrift om arbeids- og utdanningsreiser",
        "Fors om arb- og utd.r."
    ),
    TILLEGGSSTØNADSFORSKRIFTEN("46", "Tilleggsstønadforskriften", "Fors om till.stø."),
    NAV_LOVEN("47", "NA V-loven", "NAV-L"),
    MIDLERTIDIG_LOV_KOMP_SELVST_OG_FRILANS(
        "48",
        "Midlertidig lov om kompensasjonsytelse for selvstendig næringsdrivende og frilansere",
        "Midl. komp selv.næ og fril"
    ),
    LØNNSKOMPENSASJON_FOR_PERMITTERTE("49", "Lønnskompensasjon for permitterte", "Lønn.komp perm"),
    MIDLERTIDIG_FORSKR_FORSKUDD_DAGPENGER(
        "50",
        "Midlertidig forskrift om forskudd på dagpenger for å avhjelpe konsekvensene av covid-19",
        "Midlertidig forskrift om forskudd på dagpenger for å avhjelpe konsekvensene av covid-19"
    ),
    BARNEBORTFØRINGSLOVEN("51", "Barnebortføringsloven", "Bbfl"),
    KONVENSON_OM_SIVILE_SIDER_VED_BARNEBORTFØRING(
        "52",
        "Konvensjon om sivile sider ved barnebortføring",
        "Konvensjon om sivile sider ved barnebortføring"
    ),
    HAAG_KONVENSJONEN("53", "Haag-konvensjonen", "Haag-konvensjonen"),
    FORSK_OPPFLG_NAV_EGEN_REGI(
        "54",
        "Forskrift om oppfølgingstjenester i Arbeids- og velferdsetatens egen regi",
        "Forskrift om oppfølgingstjenester i Arbeids- og velferdsetatens egen regi"
    ),
    FORSKRIFT_OM_ETTERGJEVING("55", "Forskrift om ettergjeving", "Forskrift om ettergjeving"),
    FORSKRIFT_OM_OPPFOSTRINGSBIDRAG(
        "56",
        "Forskrift om fastsetting og endring av oppfostringsbidrag etter lov om barneverntjenester",
        "Forskrift om oppfostringsbidrag"
    ),
    FORSKRIFT_OM_LØNNSPLIKT_UNDER_PERMITTERING(
        "57",
        "Forskrift om lønnsplikt under permittering",
        "Forskrift om lønnsplikt under permittering"
    ),
    HOVEDNUMMER_42_TRYGDEAVTALER("58", "Hovednummer 42 - Trygdeavtaler", "Hnr. 42"),
    DEKNINGSLOVEN("59", "Dekningsloven", "Dekningsloven"),

    UKJENT("999", "Ukjent", "Ukjent")
}