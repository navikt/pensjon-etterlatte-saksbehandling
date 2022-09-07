package no.nav.etterlatte.medl.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Medlemsskapsunntak(
    /*
    Den funksjonelle ID'en til et medlemskapsunntak.
     */
    val unntakId: Long,

    /*
    Den naturlige identen som medlemskapsunntaket er lagret på.
     */
    val ident: String,

    /*
    Startdatoen for perioden til medlemskapsunntaket, på ISO-8601 format.
     */
    val fraOgMed: LocalDate,

    /*
    Sluttdatoen for perioden til medlemskapsunntaket, på ISO-8601 format.
     */
    val tilOgMed: LocalDate,

    /*
        Sluttdatoen for perioden til medlemskapsunntaket.
        Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/PeriodestatusMedl\" target=\"_blank\">PeriodestatusMedl</a>
     */
    val status: String,

    /*
        Dersom statusen på medlemskapsunntaket ikke er gyldig vil dette feltet beskrive hvorfor.
        Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/StatusaarsakMedl\" target=\"_blank\">StatusaarsakMedl</a>"
     */
    val statusaarsak: String,

    /*
        Dekningsgraden for dette medlemskapsunntaket.
        Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/DekningMedl\" target=\"_blank\">DekningMedl</a>"
     */
    val dekning: String,

    /*
        Hvorvidt dekningen for medlemskapsunntaket har en helsedel.
     */
    val helsedel: Boolean,

    /*
        Beskriver hvorvidt dette medlemskapsunntaket handler om et medlemskap i folketrygden eller ikke.", required = true
     */
    val medlem: Boolean,

    /*
        Landet dette medlemskapsunntaket gjelder for.<br/>Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/Landkoder\" target=\"_blank\">Landkoder</a>"
     */
    val lovvalgsland: String,

    /*
        Lovvalget for dette medlemskapsunntaket.<br/>Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/LovvalgMedl\" target=\"_blank\">LovvalgMedl</a>", required = true
     */
    val lovvalg: String,

    /*
        Grunnlaget for dette medlemskapsunntaket.<br/>Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/GrunnlagMedl\" target=\"_blank\">GrunnlagMedl</a>", required = true
     */
    val grunnlag: String,

    /*
        Metadata om medlemskapsunntaket, slik som når det ble opprettet og sist endret."
     */
    val sporingsinformasjon: Sporingsinformasjon,

    /*
        Medlemskapsunntak som kommer fra Lånekassen har studieinformasjon."
     */
    val studieinformasjon: Studieinformasjon
)


data class Sporingsinformasjon(
    /*
    Versjonsnummeret på dette medlemskapsunntaket. Dette sier noe om hvor mange ganger unntaket har blitt endret.",
    */
    val versjon: Int = 0,

    /*
    Når dette medlemskapsunntaket ble registrert, på ISO-8601-format.")
    */
    val registrert: LocalDate? = null,

    /*
    Når det ble tatt en beslutning om dette medlemskapsunntaket, på ISO-8601-format.")
    */
    val besluttet: LocalDate? = null,

    /*
    Hvilket system medlemskapsunntaket kommer fra.
    <br/>Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/KildesystemMedl\" target=\"_blank\">KildesystemMedl</a>",
    */
    val kilde: String? = null,

    /*
    I hvilken form søknaden om medlemskapsunntak kom til kilden.
    <br/>Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/KildedokumentMedl\" target=\"_blank\">KildedokumentMedl</a>")
    */
    val kildedokument: String? = null,

    /*
    Når medlemskapsunntaket ble opprettet, på ISO-8601-format.
    */
    val opprettet: LocalDateTime? = null,

    /*
    Hvem eller hva som opprettet medlemskapsunntaket.
    */
    val opprettetAv: String? = null,

    /*
    Når medlemskapsunntaket sist ble endret, på ISO-8601-format.
    */
    val sistEndret: LocalDateTime? = null,

    /*
    Hvem eller hva som sist endret medlemskapsunntaket.
    */
    val sistEndretAv: String? = null
)


data class Studieinformasjon(
    /*
    Hvilket land studenten er statsborger i.
    Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/Landkoder\" target=\"_blank\">Landkoder</a>
    */
    val statsborgerland: String? = null,

    /*
    Hvilket land studenten studerer i.
    Kodeverk: <a href=\"https://kodeverk-web.nais.adeo.no/kodeverksoversikt/kodeverk/Landkoder\" target=\"_blank\">Landkoder</a>
    */
    val studieland: String? = null,

    /*
    Hvorvidt studenten er deltidsstudent.
    */
    val delstudie: Boolean? = null,

    /*
    Om søknaden om studielån er blitt innvilget av Lånekassen.
    */
    val soeknadInnvilget: Boolean? = null
)
