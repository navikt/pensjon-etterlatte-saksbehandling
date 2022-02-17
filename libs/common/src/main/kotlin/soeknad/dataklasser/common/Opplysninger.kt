package no.nav.etterlatte.libs.common.soeknad.dataklasser.common

import java.time.LocalDate

data class Utenlandsadresse(
    val land: Opplysning<String>,
    val adresse: Opplysning<String>
)

data class UtbetalingsInformasjon(
    val kontonummer: Opplysning<String>? = null,
    val utenlandskBankNavn: Opplysning<String>? = null,
    val utenlandskBankAdresse: Opplysning<String>? = null,
    val iban: Opplysning<String>? = null,
    val swift: Opplysning<String>? = null,
    val skattetrekk: BetingetOpplysning<Svar, Opplysning<String>?>? = null
)

data class Kontaktinfo(
    val epost: Opplysning<String>,
    val telefonnummer: Opplysning<String>
)

enum class BankkontoType { NORSK, UTENLANDSK }
enum class InntektType { ARBEIDSINNTEKT, PENSJON, KAPITALINNTEKT, ANDRE_YTELSER }
enum class ForholdTilAvdoedeType { GIFT, SEPARERT, SAMBOER, SKILT, TIDLIGERE_SAMBOER }
enum class OppholdUtlandType { BODD, ARBEIDET }
enum class JobbStatusType { ARBEIDSTAKER, SELVSTENDIG, UNDER_UTDANNING, INGEN }
enum class StillingType { FAST, MIDLERTIDIG, SESONGARBEID }
enum class OmsorgspersonType { GJENLEVENDE, VERGE, ANNET }
enum class SivilstatusType { INGEN, EKTESKAP, SAMBOERSKAP }

data class SamboerInntekt(
    val inntektstype: Opplysning<List<InntektType>>,
    val samletBruttoinntektPrAar: Opplysning<String>,
)

data class ForholdTilAvdoede(
    val relasjon: Opplysning<ForholdTilAvdoedeType>,
    val datoForInngaattPartnerskap: Opplysning<LocalDate>? = null,
    val datoForInngaattSamboerskap: Opplysning<LocalDate>? = null,
    val datoForSkilsmisse: Opplysning<LocalDate>? = null,
    val datoForSamlivsbrudd: Opplysning<LocalDate>? = null,
    val fellesBarn: Opplysning<Svar>?,
    val samboereMedFellesBarnFoerGiftemaal: Opplysning<Svar>? = null,
    val tidligereGift: Opplysning<Svar>? = null,
    val omsorgForBarn: Opplysning<Svar>? = null,
    val mottokBidrag: Opplysning<Svar>? = null, // Finner ikke igjen
    val mottokEktefelleBidrag: Opplysning<Svar>? = null, // Finner ikke igjen?
)

data class Utenlandsopphold(
    val land: Opplysning<String>,
    val fraDato: Opplysning<LocalDate>?,
    val tilDato: Opplysning<LocalDate>?,
    val oppholdsType: Opplysning<List<OppholdUtlandType>>,
    val medlemFolketrygd: Opplysning<Svar>,
    val pensjonsutbetaling: Opplysning<String>?
)

data class Naeringsinntekt(
    val naeringsinntektPrAarFoerDoedsfall: Opplysning<String>?,
    val naeringsinntektVedDoedsfall: Opplysning<Svar>?
)

typealias AarstallForMilitaerTjeneste = String

data class ArbeidOgUtdanning(
    val dinSituasjon: Opplysning<List<JobbStatusType>>,
    val arbeidsforhold: Opplysning<List<Arbeidstaker>>?,
    val selvstendig: Opplysning<List<SelvstendigNaeringsdrivende>>?,
    val utdanning: Opplysning<Utdanning>?,
    val annet: Opplysning<String>?,
)

data class Utdanning(
    val navn: Opplysning<String>,
    val startDato: Opplysning<LocalDate>,
    val sluttDato: Opplysning<LocalDate>
)

typealias AnnenUtdanning = String

enum class HoeyesteUtdanning {
    GRUNNSKOLE,
    VIDEREGAAENDE,
    FAGBREV,
    UNIVERSITET_OPPTIL_4_AAR,
    UNIVERSITET_OVER_4_AAR,
    INGEN,
    ANNEN
}

typealias EndretInntektBegrunnelse = String

data class SelvstendigNaeringsdrivende(
    val firmanavn: Opplysning<String>,
    val orgnr: Opplysning<String>,
    val endretInntekt: BetingetOpplysning<Svar, Opplysning<EndretInntektBegrunnelse>>
)

data class Arbeidstaker(
    val arbeidsgiver: Opplysning<String>,
    val ansettelsesforhold: Opplysning<StillingType>,
    val stillingsprosent: Opplysning<String>,
    val endretInntekt: BetingetOpplysning<Svar, Opplysning<EndretInntektBegrunnelse>>
)

enum class Ytelser {
    DAGPENGER,
    SYKEPENGER,
    PLEIEPENGER,
    SVANGERSKAPSPENGER,
    FORELDREPENGER,
    ARBEIDSAVKLARINGSPENGER,
    KVALIFISERINGSSTOENAD,
    KOMMUNAL_OMSORGSSTONAD,
    FOSTERHJEMSGODTGJOERING,
    OMSORGSPENGER,
    OPPLAERINGSPENGER
}

typealias Pensjonsordning = String

data class AndreYtelser(
    val kravOmAnnenStonad: BetingetOpplysning<Svar, Opplysning<Ytelser>?>,
    val annenPensjon: BetingetOpplysning<Svar, Opplysning<Pensjonsordning>?>,
    val pensjonUtland: BetingetOpplysning<Svar, PensjonUtland?>
)

data class PensjonUtland(
    val pensjonsType: Opplysning<String>?,
    val land: Opplysning<String>?,
    val bruttobeloepPrAar: Opplysning<String>?,
)

data class OppholdUtland(
    val land: Opplysning<String>,
    val medlemFolketrygd: Opplysning<Svar>
)
